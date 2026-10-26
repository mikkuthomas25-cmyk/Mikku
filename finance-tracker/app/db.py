"""SQLite storage. One file, no server — your financial data stays on your machine."""
from __future__ import annotations

import os
import sqlite3
from contextlib import contextmanager

SCHEMA = """
CREATE TABLE IF NOT EXISTS transactions (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    sms_hash    TEXT UNIQUE,
    txn_date    TEXT NOT NULL,
    amount      REAL NOT NULL,
    direction   TEXT NOT NULL CHECK (direction IN ('debit', 'credit')),
    merchant    TEXT NOT NULL,
    category    TEXT NOT NULL,
    account     TEXT,
    ref         TEXT,
    source      TEXT NOT NULL DEFAULT 'sms',
    raw         TEXT,
    created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_txn_date ON transactions (txn_date);
CREATE UNIQUE INDEX IF NOT EXISTS idx_txn_ref
    ON transactions (ref, direction, amount) WHERE ref IS NOT NULL;

CREATE TABLE IF NOT EXISTS merchant_rules (
    merchant  TEXT PRIMARY KEY,   -- lowercased
    category  TEXT NOT NULL
);
"""


def db_path() -> str:
    return os.environ.get("FINTRACK_DB", os.path.join(os.path.dirname(__file__), "..", "fintrack.db"))


@contextmanager
def connect():
    conn = sqlite3.connect(db_path())
    conn.row_factory = sqlite3.Row
    try:
        yield conn
        conn.commit()
    finally:
        conn.close()


def init_db() -> None:
    with connect() as conn:
        conn.executescript(SCHEMA)


def load_rules(conn) -> dict[str, str]:
    return {r["merchant"]: r["category"] for r in conn.execute("SELECT * FROM merchant_rules")}
