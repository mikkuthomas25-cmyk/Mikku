"""Optional Claude-powered features. Everything here is a no-op without ANTHROPIC_API_KEY.

Privacy: categorization sends only merchant names; insights send only
aggregated totals. Raw SMS text is sent only for the parse fallback, which is
off unless FINTRACK_AI_PARSE=1.
"""
from __future__ import annotations

import json
import logging
import os
from typing import Literal

from pydantic import BaseModel

from .categorizer import CATEGORIES

log = logging.getLogger("fintrack.ai")
MODEL = os.environ.get("FINTRACK_MODEL", "claude-opus-5-5")

Category = Literal[tuple(CATEGORIES)]  # type: ignore[valid-type]


class MerchantCategory(BaseModel):
    merchant: str
    category: Category  # type: ignore[valid-type]


class CategoryBatch(BaseModel):
    results: list[MerchantCategory]


class ExtractedTxn(BaseModel):
    is_transaction: bool
    amount: float | None
    direction: Literal["debit", "credit"] | None
    merchant: str | None
    date: str | None  # yyyy-mm-dd


def enabled() -> bool:
    return bool(os.environ.get("ANTHROPIC_API_KEY") or os.environ.get("ANTHROPIC_AUTH_TOKEN"))


def parse_enabled() -> bool:
    return enabled() and os.environ.get("FINTRACK_AI_PARSE") == "1"


def _client():
    import anthropic
    return anthropic.Anthropic()


def _parse(prompt: str, schema: type[BaseModel], effort: str = "low"):
    resp = _client().beta.messages.parse(
        model=MODEL,
        max_tokens=4000,
        betas=["server-side-fallback-2026-07-01"],
        fallbacks="default",
        output_config={"effort": effort},
        messages=[{"role": "user", "content": prompt}],
        output_format=schema,
    )
    if resp.stop_reason == "refusal":
        return None
    return resp.parsed_output


def categorize_merchants(merchants: list[str]) -> dict[str, str]:
    """Map each merchant name to a category. Returns {} if AI is off or the call fails."""
    if not enabled() or not merchants:
        return {}
    prompt = (
        "These are merchant / payee names taken from Indian bank and UPI transaction "
        "SMS. Assign each one the single best spending category. Names may be "
        "truncated, a UPI handle, or a person's name (use Transfers for people).\n\n"
        f"Categories: {', '.join(CATEGORIES)}\n\nMerchants:\n"
        + "\n".join(f"- {m}" for m in merchants)
    )
    try:
        out = _parse(prompt, CategoryBatch)
    except Exception:
        log.exception("AI categorization failed")
        return {}
    return {r.merchant: r.category for r in out.results} if out else {}


def extract_transaction(sms: str) -> ExtractedTxn | None:
    """Fallback for SMS the rule parser couldn't read."""
    if not parse_enabled():
        return None
    prompt = (
        "Extract the completed bank/card/UPI transaction from this SMS. If it is an OTP, "
        "promotion, payment request, reminder, or failed transaction, set "
        "is_transaction=false. direction is 'debit' when money left the user's account.\n\n"
        f"SMS:\n{sms}"
    )
    try:
        out = _parse(prompt, ExtractedTxn)
    except Exception:
        log.exception("AI SMS extraction failed")
        return None
    if not out or not out.is_transaction or not out.amount or not out.direction:
        return None
    return out


def monthly_insights(summary: dict) -> str:
    """Plain-language analysis of one month's aggregated numbers."""
    if not enabled():
        return ("AI insights are off. Set ANTHROPIC_API_KEY on the server to enable them.")
    import anthropic
    prompt = (
        "You are a personal finance coach. Below is one month of my spending, "
        "aggregated from bank SMS (amounts in INR). Give me:\n"
        "1. Where my money went, in two or three sentences.\n"
        "2. Three specific, realistic ways to save next month, with estimated rupee amounts.\n"
        "3. Anything unusual (spikes, a category growing fast, many small payments adding up).\n"
        "Plain text, short paragraphs, no markdown headings. Don't invent data that isn't here.\n\n"
        + json.dumps(summary, indent=1)
    )
    try:
        resp = _client().beta.messages.create(
            model=MODEL,
            max_tokens=4000,
            betas=["server-side-fallback-2026-07-01"],
            fallbacks="default",
            output_config={"effort": "medium"},
            messages=[{"role": "user", "content": prompt}],
        )
    except anthropic.APIError as e:
        return f"Couldn't reach Claude: {e.__class__.__name__}."
    if resp.stop_reason == "refusal":
        return "Claude declined to answer this request."
    return "".join(b.text for b in resp.content if b.type == "text").strip()
