# FinTrack: personal finance tracker that reads your bank SMS

FinTrack turns the transaction SMS your bank already sends ("Rs.250 debited from
A/c XX1234 to VPA swiggy@icici…") into a dashboard. It shows what you spent today,
day by day, by category and by merchant, and where your money went at the end of
the month. Claude can optionally categorize unfamiliar merchants and write a
monthly "where could I save" analysis.

It is self-hosted. Your data lives in one SQLite file on your own machine.

## Why SMS and not a bank connection?

- **Direct bank connection (India):** this goes through the RBI **Account
  Aggregator** framework. Only a registered *Financial Information User*, which is
  a regulated entity, can pull data through it. A personal app can't, unless it
  goes through a licensed partner with a business agreement. (Outside India,
  Plaid/GoCardless-style APIs have similar business-onboarding requirements.)
  This could be added later; it isn't something one person can switch on.
- **SMS** covers almost every account and card you have: every debit, UPI
  payment and card swipe produces one. It's the practical route.
- **Platform reality:**
  - **iPhone:** no app can read your SMS. Apple doesn't allow it. The workaround
    below uses a Shortcuts automation that *forwards* matching messages.
  - **Android:** apps can read SMS. Google Play only allows that permission for
    default-SMS apps, but you can use it for personal use (a forwarder app, or
    your own sideloaded app later).

## Run it

```bash
cd finance-tracker
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

export FINTRACK_TOKEN="$(python3 -c 'import secrets;print(secrets.token_urlsafe(24))')"
echo "Your token: $FINTRACK_TOKEN"          # the phone and dashboard need this
export ANTHROPIC_API_KEY=sk-ant-...           # optional: turns on AI features

uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Open http://localhost:8000. To try it with demo data, go to **Add transactions → Import
backup** and choose `samples/sample_sms.txt`.

> **Always set `FINTRACK_TOKEN`** when the server is reachable from anything other
> than your own computer. Without it the API is open. If you expose it to the
> internet, put it behind HTTPS (for example a Cloudflare Tunnel, Tailscale, or a
> small VPS with Caddy).

## Get your SMS into it

### 1. History: import past messages once
- **Android:** install *SMS Backup & Restore*, export your messages to an `.xml`
  file, then import it in **Add transactions → Import backup**. It only imports
  received messages, and duplicates are skipped, so importing again is safe.
- **iPhone:** iOS has no SMS export. Copy and paste messages into **Paste SMS**,
  separated by blank lines, or start tracking from today.

### 2. Live: every new transaction SMS, automatically

Your phone needs to reach the server: the same Wi-Fi (`http://<laptop-LAN-IP>:8000`)
or a public HTTPS URL.

**iPhone (Shortcuts app):**
1. Shortcuts → **Automation** → **+** → **Message**.
2. *Message Contains*: `debited` (make extra automations for `credited`,
   `spent` and `sent Rs`). Choose **Run Immediately**.
3. Action **Get Contents of URL**:
   - URL: `https://your-server/api/ingest`
   - Method: `POST`, Headers: `X-Api-Token` = your token
   - Request Body: JSON, key `body` = *Shortcut Input → Message Content*
     (it's a magic variable).

**Android:** use any app that forwards SMS to a webhook (for example the
open-source *SMS to URL Forwarder*, or Tasker). Send a POST to `/api/ingest`
with JSON `{"body": "<message text>"}` and the header `X-Api-Token: <token>`.
Filter to bank senders if the app supports it. Non-transaction messages are
ignored anyway.

API test without a phone:

```bash
curl -X POST localhost:8000/api/ingest -H "X-Api-Token: $FINTRACK_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"body":"Rs.250.00 debited from A/c XX1234 on 12-10-26 to VPA swiggy@icici. UPI Ref 628512345678"}'
```

### 3. Cash
**Add transactions → Cash / manual** covers anything without an SMS.

## What the dashboard shows
- Spent, received and net for the month, today's spend, daily average, change vs last month,
  and how much "small payments" (₹200 or less) add up to.
- Daily spending chart. Tap a day to see that day's transactions.
- Category and top-merchant breakdowns. Tap one to filter the list.
- Editable categories. Changing one teaches the app: that merchant keeps the
  category from then on.
- **AI insights:** a plain-language read of the month with specific savings ideas.

## How it works
| Piece | File |
|---|---|
| SMS → amount, debit/credit, date, merchant, account, UPI ref | `app/parser.py` (rule-based, tuned for Indian bank formats) |
| Category from merchant keywords plus your own corrections | `app/categorizer.py` |
| Claude: categorize leftover "Other" merchants, monthly insights, optional parse fallback | `app/ai.py` |
| API and storage | `app/main.py`, `app/db.py` (SQLite) |
| Dashboard | `app/static/index.html` (no build step, no external scripts) |

**Ignored:** OTPs, promos, "bill is due" reminders, UPI collect requests and failed
transactions. **Deduplicated:** the same SMS twice, or the same UPI reference arriving
in two different SMS (bank + UPI app).
**Credit card bills:** card purchases are already counted, so paying the card bill
from your bank account is tagged **Self Transfer** and left out of totals. Do the same
for moving money between your own accounts.

## AI and privacy
AI features only run when `ANTHROPIC_API_KEY` is set. They use Claude Opus 5.5
by default; override with `FINTRACK_MODEL`.
- Categorization sends **only merchant names**.
- Insights send **only monthly aggregates** (category totals and top merchants),
  never raw SMS.
- `FINTRACK_AI_PARSE=1` turns on a fallback that sends the **full text** of SMS the
  rule parser couldn't read to Claude. It's off by default.

## Known limits
- Bank SMS formats vary. If a message from your bank is parsed wrong, add it to
  `samples/sample_sms.txt` and `tests/test_parser.py` and adjust `app/parser.py`.
  Or turn on the AI parse fallback.
- Some refunds of failed transactions are skipped, because "failed" messages are
  filtered as noise.
- Single user, by design.

## Tests
```bash
pytest -q
```
