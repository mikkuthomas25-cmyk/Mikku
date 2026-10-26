"""Parse bank / UPI / card transaction SMS into structured transactions.

Rule-based, tuned for Indian bank message formats (Rs./INR/₹, A/c XX1234,
UPI VPAs, dd-mm-yy dates). Anything that does not look like a completed
transaction (OTPs, promos, payment requests, due reminders) returns None.
"""
from __future__ import annotations

import hashlib
import re
from dataclasses import dataclass, asdict
from datetime import date, datetime

AMOUNT_RE = re.compile(r"(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)", re.I)

DEBIT_WORDS = r"debited|spent|sent|paid|withdrawn|withdrawal|purchase|debit(?:ed)?|txn of|transferred to"
CREDIT_WORDS = r"credited|received|deposited|refund(?:ed)?|reversed|cashback of"
DEBIT_RE = re.compile(rf"\b(?:{DEBIT_WORDS})\b", re.I)
CREDIT_RE = re.compile(rf"\b(?:{CREDIT_WORDS})\b", re.I)

# Messages that mention money but are not completed transactions.
NOISE_RE = re.compile(
    r"\botp\b|one[\s-]time password|verification code|"
    r"will be debited|is due|due date|due on|requested money|collect request|"
    r"has requested|payment request|minimum amount due|"
    r"\boffer\b|\bwin\b|\bcongratulations\b|pre-?approved|\bloan of\b|"
    r"failed|declined|unsuccessful|insufficient",
    re.I,
)

ACCOUNT_RE = re.compile(
    r"(?:a/c|acct|account|card)\s*(?:no\.?)?\s*(?:(?:ending\s*(?:with|in)?\s*)[x*]*|[x*]+)\s*(\d{3,6})",
    re.I,
)
REF_RE = re.compile(
    r"(?:upi\s*ref(?:erence)?|ref(?:erence)?|utr|rrn|txn\s*id)\s*(?:no\.?|num(?:ber)?|#)?\s*[:.\-]?\s*([A-Za-z0-9]{6,})",
    re.I,
)

_STOP = r"(?=\s+on\b|\s+ref\b|\s+upi\b|\s+avl\b|\s+avail|\s+bal\b|\s+via\b|\s+using\b|\s+not\s+you|\s+if\s+not|[.,;()]|\s*-\s|$)"
ATM_RE = re.compile(r"\batm\b.{0,30}?(?:wdl|withdrawa?l|cash)|(?:cash\s+)?withdrawn\s+at\s+atm|\bat\s+atm\b", re.I)
MERCHANT_PATTERNS = [
    re.compile(r"\b(?:to|from)\s+(?:vpa\s+)?([\w.\-]+@[a-z][\w\-]*)", re.I),
    re.compile(rf"\bat\s+([A-Za-z0-9][\w .&'*/@\-]{{1,40}}?){_STOP}", re.I),
    re.compile(rf"\bto\s+([A-Za-z0-9][\w .&'*/@\-]{{1,40}}?){_STOP}", re.I),
    re.compile(rf"\bby\s+(?:neft|imps|rtgs|upi)?\s*(?:from\s+)?([A-Za-z0-9][\w .&'*/@\-]{{1,40}}?){_STOP}", re.I),
    re.compile(rf"\bfrom\s+([A-Za-z0-9][\w .&'*/@\-]{{1,40}}?){_STOP}", re.I),
    re.compile(r"\binfo\s*[:\-]\s*([^.]{2,40})", re.I),
]
# Phrases the merchant regexes can capture that are really our own account.
SELF_REF_RE = re.compile(r"^(?:your\s+)?(?:a/c|acct|account|card|bank|.*\bbank\s+a/?c)\b|^(?:[a-z]+\s+)?bank\b.*[x*]\d", re.I)

MONTHS = {m: i for i, m in enumerate(
    ["jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"], 1)}
DATE_NUMERIC_RE = re.compile(r"\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})\b")
DATE_ISO_RE = re.compile(r"\b(\d{4})-(\d{2})-(\d{2})\b")
DATE_TEXT_RE = re.compile(r"\b(\d{1,2})[-\s]?([A-Za-z]{3})[a-z]*[-\s,]*(\d{2,4})\b")


@dataclass
class ParsedTransaction:
    amount: float
    direction: str            # "debit" | "credit"
    txn_date: str             # ISO yyyy-mm-dd
    merchant: str
    account: str | None
    ref: str | None
    sms_hash: str

    def to_dict(self) -> dict:
        return asdict(self)


def sms_hash(body: str) -> str:
    normalized = re.sub(r"\s+", " ", body.strip().lower())
    return hashlib.sha256(normalized.encode()).hexdigest()[:32]


def _year(y: str) -> int:
    n = int(y)
    return n + 2000 if n < 100 else n


def parse_date(text: str, fallback: date) -> date:
    m = DATE_ISO_RE.search(text)
    if m:
        try:
            return date(int(m[1]), int(m[2]), int(m[3]))
        except ValueError:
            pass
    m = DATE_TEXT_RE.search(text)
    if m and m[2].lower() in MONTHS:
        try:
            return date(_year(m[3]), MONTHS[m[2].lower()], int(m[1]))
        except ValueError:
            pass
    m = DATE_NUMERIC_RE.search(text)
    if m:
        # Indian banks use day-first dates.
        try:
            return date(_year(m[3]), int(m[2]), int(m[1]))
        except ValueError:
            pass
    return fallback


def clean_merchant(raw: str) -> str:
    s = raw.strip(" .:-*")
    if "@" in s:                       # UPI VPA: swiggy.upi@icici -> swiggy
        s = s.split("@")[0]
        s = re.sub(r"[._-]?(?:upi|pay|payments?)$", "", s, flags=re.I)
        s = re.sub(r"^\d{6,}$", "UPI transfer", s)   # phone-number VPAs
    s = re.sub(r"[._]+", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    s = re.sub(r"(?:\s+(?:com|in|india|pvt|private|ltd|limited|llp))+$", "", s, flags=re.I) or s
    return s.title() if s.isupper() or s.islower() else s


def extract_merchant(body: str) -> str:
    if ATM_RE.search(body):
        return "ATM Withdrawal"
    for pat in MERCHANT_PATTERNS:
        for m in pat.finditer(body):
            cand = m.group(1).strip()
            if not cand or SELF_REF_RE.search(cand) or AMOUNT_RE.match(cand):
                continue
            if re.fullmatch(r"[\d\s/.-]+", cand):
                continue
            return clean_merchant(cand)
    return "Unknown"


def parse_sms(body: str, received_at: datetime | None = None) -> ParsedTransaction | None:
    """Return a ParsedTransaction, or None if the SMS isn't a completed transaction."""
    if not body or NOISE_RE.search(body):
        return None
    amt = AMOUNT_RE.search(body)
    if not amt:
        return None
    debit = DEBIT_RE.search(body)
    credit = CREDIT_RE.search(body)
    if not debit and not credit:
        return None
    # Whichever verb appears first describes *our* side of the transaction
    # ("debited from your a/c ... credited to X" is a debit).
    if debit and (not credit or debit.start() < credit.start()):
        direction = "debit"
    else:
        direction = "credit"

    try:
        amount = float(amt.group(1).replace(",", ""))
    except ValueError:
        return None
    if amount <= 0:
        return None

    fallback = (received_at or datetime.now()).date()
    acct = ACCOUNT_RE.search(body)
    ref = REF_RE.search(body)
    return ParsedTransaction(
        amount=round(amount, 2),
        direction=direction,
        txn_date=parse_date(body, fallback).isoformat(),
        merchant=extract_merchant(body),
        account=acct.group(1) if acct else None,
        ref=ref.group(1) if ref else None,
        sms_hash=sms_hash(body),
    )


def looks_financial(body: str) -> bool:
    """Loose check used to decide whether an unparsed SMS is worth an AI retry."""
    return bool(AMOUNT_RE.search(body)) and not NOISE_RE.search(body) and bool(
        re.search(r"a/c|acct|account|card|upi|bank", body, re.I))
