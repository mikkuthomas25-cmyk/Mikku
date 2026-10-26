"""FinTrack API + dashboard.

Run:  uvicorn app.main:app --host 127.0.0.1 --port 8000
"""
from __future__ import annotations

import calendar
import os
import secrets
import sqlite3
import xml.etree.ElementTree as ET
from contextlib import asynccontextmanager
from datetime import date, datetime, timedelta
from pathlib import Path

from fastapi import Depends, FastAPI, File, Header, HTTPException, Query, UploadFile
from fastapi.responses import FileResponse
from pydantic import BaseModel

from . import ai
from .categorizer import CATEGORIES, EXCLUDED_FROM_TOTALS, categorize
from .db import connect, init_db, load_rules
from .parser import looks_financial, parse_sms, sms_hash

STATIC = Path(__file__).parent / "static"
MAX_IMPORT_BYTES = 50 * 1024 * 1024

@asynccontextmanager
async def lifespan(_app):
    init_db()
    yield


app = FastAPI(title="FinTrack", lifespan=lifespan)


def require_token(x_api_token: str | None = Header(default=None), token: str | None = Query(default=None)):
    expected = os.environ.get("FINTRACK_TOKEN")
    if not expected:
        return
    given = x_api_token or token or ""
    if not secrets.compare_digest(given, expected):
        raise HTTPException(status_code=401, detail="Invalid or missing API token")


# ---------------------------------------------------------------- ingestion

def ingest_one(conn, body: str, received_at: datetime | None, source: str, rules: dict) -> tuple[str, dict | None]:
    """Returns (status, row) where status is added | duplicate | ignored."""
    body = (body or "").strip()
    parsed = parse_sms(body, received_at)
    if parsed:
        rec = parsed.to_dict()
    elif looks_financial(body) and (x := ai.extract_transaction(body)):
        rec = {
            "amount": round(x.amount, 2), "direction": x.direction,
            "txn_date": x.date or (received_at or datetime.now()).date().isoformat(),
            "merchant": x.merchant or "Unknown", "account": None, "ref": None,
            "sms_hash": sms_hash(body),
        }
        source = f"{source}+ai"
    else:
        return "ignored", None

    rec["category"] = categorize(rec["merchant"], rec["direction"], rules)
    rec["source"] = source
    rec["raw"] = body
    try:
        cur = conn.execute(
            """INSERT INTO transactions
               (sms_hash, txn_date, amount, direction, merchant, category, account, ref, source, raw)
               VALUES (:sms_hash, :txn_date, :amount, :direction, :merchant, :category,
                       :account, :ref, :source, :raw)""",
            rec,
        )
    except sqlite3.IntegrityError:
        return "duplicate", None
    rec["id"] = cur.lastrowid
    return "added", rec


def ai_fill_other(conn, rows: list[dict]) -> None:
    """Ask Claude to categorize debits that keyword rules left as 'Other'."""
    pending = sorted({r["merchant"] for r in rows if r["category"] == "Other" and r["merchant"] != "Unknown"})
    if not pending:
        return
    mapping = ai.categorize_merchants(pending)
    for r in rows:
        cat = mapping.get(r["merchant"])
        if r["category"] == "Other" and cat and cat != "Other":
            conn.execute("UPDATE transactions SET category=? WHERE id=?", (cat, r["id"]))
            r["category"] = cat


class SmsIn(BaseModel):
    body: str
    sender: str | None = None
    received_at: datetime | None = None


@app.post("/api/ingest", dependencies=[Depends(require_token)])
def ingest(msg: SmsIn):
    """Endpoint for phone automations (iOS Shortcut / Android SMS forwarder)."""
    with connect() as conn:
        status, row = ingest_one(conn, msg.body, msg.received_at, "sms", load_rules(conn))
        if row:
            ai_fill_other(conn, [row])
    return {"status": status, "transaction": _public(row)}


def _split_messages(raw: bytes, filename: str) -> list[tuple[str, datetime | None]]:
    text = raw.decode("utf-8", errors="replace")
    if filename.lower().endswith(".xml") or text.lstrip().startswith("<?xml") or "<smses" in text[:500]:
        # "SMS Backup & Restore" (Android) export format.
        out = []
        for el in ET.fromstring(text).iter("sms"):
            if el.get("type", "1") != "1":      # inbox only
                continue
            ts = el.get("date")
            when = datetime.fromtimestamp(int(ts) / 1000) if ts and ts.isdigit() else None
            out.append((el.get("body", ""), when))
        return out
    # Plain text: messages separated by blank lines.
    return [(chunk.strip(), None) for chunk in text.replace("\r\n", "\n").split("\n\n") if chunk.strip()]


@app.post("/api/import", dependencies=[Depends(require_token)])
async def import_file(file: UploadFile = File(...)):
    raw = await file.read(MAX_IMPORT_BYTES + 1)
    if len(raw) > MAX_IMPORT_BYTES:
        raise HTTPException(413, "File too large (50 MB max)")
    try:
        messages = _split_messages(raw, file.filename or "")
    except ET.ParseError as e:
        raise HTTPException(400, f"Could not read XML: {e}")
    counts = {"added": 0, "duplicate": 0, "ignored": 0}
    added = []
    with connect() as conn:
        rules = load_rules(conn)
        for body, when in messages:
            status, row = ingest_one(conn, body, when, "import", rules)
            counts[status] += 1
            if row:
                added.append(row)
        ai_fill_other(conn, added)
    return {"total": len(messages), **counts}


class ManualTxn(BaseModel):
    amount: float
    direction: str = "debit"
    merchant: str
    txn_date: date
    category: str | None = None


@app.post("/api/transactions", dependencies=[Depends(require_token)])
def add_manual(t: ManualTxn):
    """For cash spends and anything that never produced an SMS."""
    if t.direction not in ("debit", "credit") or t.amount <= 0:
        raise HTTPException(400, "direction must be debit/credit and amount > 0")
    if t.category and t.category not in CATEGORIES:
        raise HTTPException(400, "Unknown category")
    with connect() as conn:
        cat = t.category or categorize(t.merchant, t.direction, load_rules(conn))
        cur = conn.execute(
            """INSERT INTO transactions (txn_date, amount, direction, merchant, category, source)
               VALUES (?, ?, ?, ?, ?, 'manual')""",
            (t.txn_date.isoformat(), round(t.amount, 2), t.direction, t.merchant.strip(), cat),
        )
        row = conn.execute("SELECT * FROM transactions WHERE id=?", (cur.lastrowid,)).fetchone()
    return _public(dict(row))


# ---------------------------------------------------------------- reading

def _month_bounds(month: str | None) -> tuple[date, date]:
    if month:
        try:
            y, m = map(int, month.split("-"))
            start = date(y, m, 1)
        except ValueError:
            raise HTTPException(400, "month must be YYYY-MM")
    else:
        start = date.today().replace(day=1)
    end = start.replace(day=calendar.monthrange(start.year, start.month)[1])
    return start, end


def _public(row: dict | None) -> dict | None:
    if row is None:
        return None
    return {k: row[k] for k in ("id", "txn_date", "amount", "direction", "merchant",
                                "category", "account", "source") if k in row}


@app.get("/api/categories")
def categories():
    return CATEGORIES


@app.get("/api/transactions", dependencies=[Depends(require_token)])
def list_transactions(month: str | None = None, day: str | None = None):
    start, end = _month_bounds(month)
    q, args = "SELECT * FROM transactions WHERE txn_date BETWEEN ? AND ?", [start.isoformat(), end.isoformat()]
    if day:
        q, args = "SELECT * FROM transactions WHERE txn_date = ?", [day]
    with connect() as conn:
        rows = conn.execute(q + " ORDER BY txn_date DESC, id DESC", args).fetchall()
    return [_public(dict(r)) | {"raw": r["raw"]} for r in rows]


class TxnPatch(BaseModel):
    category: str
    apply_to_merchant: bool = True


@app.patch("/api/transactions/{txn_id}", dependencies=[Depends(require_token)])
def update_category(txn_id: int, patch: TxnPatch):
    if patch.category not in CATEGORIES:
        raise HTTPException(400, "Unknown category")
    with connect() as conn:
        row = conn.execute("SELECT * FROM transactions WHERE id=?", (txn_id,)).fetchone()
        if not row:
            raise HTTPException(404, "Not found")
        if patch.apply_to_merchant:
            # Remember the choice: this merchant gets this category from now on.
            conn.execute("INSERT OR REPLACE INTO merchant_rules (merchant, category) VALUES (?, ?)",
                         (row["merchant"].lower(), patch.category))
            conn.execute("UPDATE transactions SET category=? WHERE lower(merchant)=? AND direction=?",
                         (patch.category, row["merchant"].lower(), row["direction"]))
        else:
            conn.execute("UPDATE transactions SET category=? WHERE id=?", (patch.category, txn_id))
    return {"ok": True}


@app.delete("/api/transactions/{txn_id}", dependencies=[Depends(require_token)])
def delete_transaction(txn_id: int):
    with connect() as conn:
        conn.execute("DELETE FROM transactions WHERE id=?", (txn_id,))
    return {"ok": True}


def build_summary(month: str | None) -> dict:
    start, end = _month_bounds(month)
    prev_end = start - timedelta(days=1)
    prev_start = prev_end.replace(day=1)
    today = date.today()
    with connect() as conn:
        rows = [dict(r) for r in conn.execute(
            "SELECT txn_date, amount, direction, merchant, category FROM transactions "
            "WHERE txn_date BETWEEN ? AND ?", (start.isoformat(), end.isoformat()))]
        excluded = ",".join("?" * len(EXCLUDED_FROM_TOTALS))
        prev_spent = conn.execute(
            "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE direction='debit' "
            f"AND category NOT IN ({excluded}) AND txn_date BETWEEN ? AND ?",
            (*EXCLUDED_FROM_TOTALS, prev_start.isoformat(), prev_end.isoformat())).fetchone()[0]

    rows = [r for r in rows if r["category"] not in EXCLUDED_FROM_TOTALS]
    debits = [r for r in rows if r["direction"] == "debit"]
    spent = sum(r["amount"] for r in debits)
    received = sum(r["amount"] for r in rows if r["direction"] == "credit")

    by_day = {(start + timedelta(days=i)).isoformat(): 0.0 for i in range((end - start).days + 1)}
    by_cat: dict[str, float] = {}
    by_merchant: dict[str, list] = {}
    for r in debits:
        by_day[r["txn_date"]] = by_day.get(r["txn_date"], 0) + r["amount"]
        by_cat[r["category"]] = by_cat.get(r["category"], 0) + r["amount"]
        agg = by_merchant.setdefault(r["merchant"], [0.0, 0])
        agg[0] += r["amount"]
        agg[1] += 1

    if start <= today <= end:
        days_elapsed = today.day
    elif today > end:
        days_elapsed = end.day
    else:
        days_elapsed = 0
    small = [r for r in debits if r["amount"] <= 200]
    return {
        "month": start.strftime("%Y-%m"),
        "spent": round(spent, 2),
        "received": round(received, 2),
        "net": round(received - spent, 2),
        "today_spent": round(by_day.get(today.isoformat(), 0), 2),
        "avg_per_day": round(spent / days_elapsed, 2) if days_elapsed else 0,
        "prev_month_spent": round(prev_spent, 2),
        "txn_count": len(rows),
        "small_payments": {"count": len(small), "total": round(sum(r["amount"] for r in small), 2)},
        "by_day": [{"date": d, "amount": round(v, 2)} for d, v in sorted(by_day.items())],
        "by_category": sorted(({"category": c, "amount": round(v, 2)} for c, v in by_cat.items()),
                              key=lambda x: -x["amount"]),
        "top_merchants": sorted(({"merchant": m, "amount": round(v[0], 2), "count": v[1]}
                                 for m, v in by_merchant.items()), key=lambda x: -x["amount"])[:10],
    }


@app.get("/api/summary", dependencies=[Depends(require_token)])
def summary(month: str | None = None):
    return build_summary(month)


@app.post("/api/insights", dependencies=[Depends(require_token)])
def insights(month: str | None = None):
    s = build_summary(month)
    if not s["txn_count"]:
        return {"enabled": ai.enabled(), "text": "No transactions this month yet."}
    s.pop("by_day")  # daily detail adds tokens without adding much signal
    return {"enabled": ai.enabled(), "text": ai.monthly_insights(s)}


@app.get("/api/status")
def status():
    return {"ai": ai.enabled(), "ai_parse": ai.parse_enabled(),
            "auth": bool(os.environ.get("FINTRACK_TOKEN"))}


@app.get("/")
def dashboard():
    return FileResponse(STATIC / "index.html")
