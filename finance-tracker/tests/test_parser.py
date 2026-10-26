from datetime import datetime
from pathlib import Path

import pytest

from app.categorizer import keyword_category
from app.parser import parse_sms

SAMPLES = (Path(__file__).parent.parent / "samples" / "sample_sms.txt").read_text().split("\n\n")

EXPECTED = [
    # amount, direction, date, merchant, account
    (250.0, "debit", "2026-10-12", "Swiggy", "1234"),
    (120.0, "debit", "2026-10-13", "Zomato", "5678"),
    (2499.0, "debit", "2026-10-13", "Amazon Pay", "9876"),
    (50000.0, "credit", "2026-10-01", "Acme Technologies", "1234"),
    (1500.0, "debit", "2026-10-05", "ATM Withdrawal", "1234"),
    (640.0, "debit", "2026-10-14", "Uber", "5678"),
    (500.0, "credit", "2026-10-15", "Rahul K", "5678"),
    (15000.0, "debit", "2026-10-03", "Landlord Sharma", "1234"),
    (899.0, "debit", "2026-10-10", "Netflix", "4321"),
    (1250.5, "debit", "2026-10-16", "Bigbasket", "1234"),
]


@pytest.mark.parametrize("body,expected", zip(SAMPLES, EXPECTED))
def test_parses_transactions(body, expected):
    t = parse_sms(body.strip())
    assert t is not None
    assert (t.amount, t.direction, t.txn_date, t.merchant, t.account) == expected


@pytest.mark.parametrize("body", SAMPLES[len(EXPECTED):])
def test_ignores_non_transactions(body):
    """OTPs, promos, bill reminders and collect requests must not become spends."""
    assert parse_sms(body.strip()) is None


def test_falls_back_to_received_date():
    t = parse_sms("Rs 99 debited from a/c XX1111 to VPA chai@paytm", datetime(2026, 9, 3, 10))
    assert t.txn_date == "2026-09-03"
    assert t.merchant == "Chai"


def test_same_sms_same_hash():
    a = parse_sms(SAMPLES[0])
    b = parse_sms("  " + SAMPLES[0].replace(" ", "  ") + "\n")
    assert a.sms_hash == b.sms_hash


@pytest.mark.parametrize("merchant,direction,cat", [
    ("Swiggy", "debit", "Food & Dining"),
    ("Bigbasket", "debit", "Groceries"),
    ("Uber", "debit", "Transport"),
    ("Netflix", "debit", "Entertainment"),
    ("ATM Withdrawal", "debit", "Cash Withdrawal"),
    ("Landlord Sharma", "debit", "Rent"),
    ("Acme Technologies", "credit", "Income"),
    ("Some Random Shop", "debit", "Other"),
])
def test_keyword_categories(merchant, direction, cat):
    assert keyword_category(merchant, direction) == cat
