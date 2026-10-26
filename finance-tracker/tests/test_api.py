from pathlib import Path

import pytest
from fastapi.testclient import TestClient

SAMPLE = Path(__file__).parent.parent / "samples" / "sample_sms.txt"


@pytest.fixture
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("FINTRACK_DB", str(tmp_path / "t.db"))
    monkeypatch.delenv("FINTRACK_TOKEN", raising=False)
    monkeypatch.delenv("ANTHROPIC_API_KEY", raising=False)
    monkeypatch.delenv("ANTHROPIC_AUTH_TOKEN", raising=False)
    from app.main import app
    with TestClient(app) as c:
        yield c


def test_import_summary_and_dedupe(client):
    files = {"file": ("s.txt", SAMPLE.read_bytes(), "text/plain")}
    r = client.post("/api/import", files=files).json()
    assert r == {"total": 14, "added": 10, "duplicate": 0, "ignored": 4}
    # Importing the same messages again adds nothing.
    r = client.post("/api/import", files=files).json()
    assert r["added"] == 0 and r["duplicate"] == 10

    s = client.get("/api/summary?month=2026-10").json()
    assert s["spent"] == pytest.approx(250 + 120 + 2499 + 1500 + 640 + 15000 + 899 + 1250.5)
    assert s["received"] == 50500
    assert s["by_category"][0] == {"category": "Rent", "amount": 15000}
    assert len(s["by_day"]) == 31
    assert next(d for d in s["by_day"] if d["date"] == "2026-10-13")["amount"] == 2619


def test_ingest_endpoint_and_learned_category(client):
    body = "Rs 349.00 debited from a/c XX1234 on 20-10-26 to VPA thirdwavecoffee@ybl. UPI Ref 628599990000"
    r = client.post("/api/ingest", json={"body": body}).json()
    assert r["status"] == "added"
    t = r["transaction"]
    assert t["merchant"] == "Thirdwavecoffee" and t["category"] == "Other"

    client.patch(f"/api/transactions/{t['id']}", json={"category": "Food & Dining"})
    # Next payment to the same merchant is categorized from the learned rule.
    r = client.post("/api/ingest", json={"body": body.replace("349.00", "199.00").replace("9990000", "9990001")}).json()
    assert r["transaction"]["category"] == "Food & Dining"

    assert client.post("/api/ingest", json={"body": "Your OTP is 123456 for Rs 500"}).json()["status"] == "ignored"


def test_same_upi_ref_from_two_sms_counts_once(client):
    a = "Rs 640.00 debited from a/c XX5678 on 14-10-26 to VPA uber@axis. UPI Ref 628511112222 -HDFC"
    b = "Paid Rs.640.00 to Uber India from A/c XX5678. UPI Ref No 628511112222"
    assert client.post("/api/ingest", json={"body": a}).json()["status"] == "added"
    assert client.post("/api/ingest", json={"body": b}).json()["status"] == "duplicate"


def test_token_required_when_set(client, monkeypatch):
    monkeypatch.setenv("FINTRACK_TOKEN", "s3cret")
    assert client.post("/api/ingest", json={"body": "x"}).status_code == 401
    assert client.post("/api/ingest", json={"body": "x"}, headers={"X-Api-Token": "s3cret"}).status_code == 200


def test_sms_backup_xml_import(client):
    xml = """<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
<smses count="2">
  <sms address="VM-HDFCBK" date="1760500000000" type="1" body="Rs.75.00 debited from a/c XX5678 to VPA rapido@ybl. UPI Ref 628577778888" />
  <sms address="Me" date="1760500000000" type="2" body="Sent Rs 10 to someone a/c XX1" />
</smses>"""
    r = client.post("/api/import", files={"file": ("backup.xml", xml.encode(), "text/xml")}).json()
    assert r["total"] == 1 and r["added"] == 1


def test_manual_cash_entry(client):
    r = client.post("/api/transactions", json={"amount": 60, "merchant": "Auto rickshaw",
                                               "txn_date": "2026-10-18", "direction": "debit"})
    assert r.status_code == 200 and r.json()["source"] == "manual"


def test_card_bill_payment_not_double_counted(client):
    client.post("/api/ingest", json={"body": "INR 2,000.00 spent on ICICI Bank Card XX9876 on 05-Oct-26 at MYNTRA. Avl Lmt INR 1,000"})
    client.post("/api/ingest", json={"body": "Rs 2,000.00 debited from A/c XX1234 on 20-10-26 to ICICI Credit Card XX9876 payment. Ref 998877665544"})
    s = client.get("/api/summary?month=2026-10").json()
    assert s["spent"] == 2000
