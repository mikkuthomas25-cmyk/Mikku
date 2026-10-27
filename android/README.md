# FinTrack for Android

The Android app reads your bank transaction SMS on the phone and shows where your money
goes: daily spend, categories, top merchants and month-over-month change. You can sign in
with Google or with an email and password. A small machine-learning model on the phone learns
your categories from your own corrections.

Everything is stored **on the phone** (one database per signed-in account). Messages are
never uploaded.

## Get the APK

Every push that changes `android/` builds the APK on GitHub:

1. Open the repo on GitHub, go to **Actions → Android APK**, and open the latest green run.
2. Under **Artifacts**, download **FinTrack-apk**. It's a zip; `FinTrack.apk` is inside.
3. Copy it to your phone and open it. Android will ask you to allow installs from that
   app (Files / Chrome). Allow it.
4. **Play Protect may warn** that the app reads SMS. That's expected for any SMS-reading
   app that isn't from the Play Store. Choose *More details → Install anyway*.

To get a permanent download link, push a tag such as `v1.0`. The APK is then attached to
a GitHub Release.

Every build is signed with the same key (`app/fintrack-debug.keystore`), so a new APK
installs over the old one and keeps your data.

## If the phone blocks SMS access

Android protects SMS for apps installed outside a store (Android 15+ "restricted settings";
Xiaomi HyperOS/MIUI applies its own block). If the permission dialog fails or does nothing,
the Home screen switches to a guide with buttons that open the right settings page:

- **Xiaomi / Redmi / POCO:** *Open SMS permission* → set **SMS** to **Allow**. If it's greyed
  out: *Open App info* → **⋮** (top-right) → **Allow restricted settings** → try again.
  Also turn on **Profile → Autostart**, or HyperOS stops FinTrack from catching new SMS in
  the background.
- **Other phones:** *Open App info* → **⋮** → **Allow restricted settings** (if shown) →
  **Permissions → SMS → Allow**.

Without SMS access the app still works: open any bank SMS → **Share → FinTrack**, and the
transaction is added. Cash spends can be added with the **+** button in Transactions.

On Google Play the sideloading restriction doesn't apply. Play allows SMS access for
*SMS-based money management* apps after its Permissions Declaration review (see below).

## Turn on Google and email login (one-time, about 10 minutes)

Login runs through Firebase Authentication, which is free for this use. Until you set it
up, the app shows **Continue without account** so you can use it straight away.

1. Go to https://console.firebase.google.com, choose **Add project** (Analytics is not needed).
2. **Add app → Android**:
   - Package name: `com.fintrack.app`
   - Debug signing certificate SHA-1 (required for Google sign-in):
     `E3:59:A8:58:2C:29:B5:A0:53:E4:14:6E:9A:A9:94:63:2B:4B:C4:6A`
3. Download **google-services.json**.
4. In Firebase, go to **Build → Authentication → Get started → Sign-in method** and enable
   **Email/Password** and **Google**.
5. After enabling Google, download `google-services.json` **again** from Project
   settings. The new file contains the OAuth client that Google sign-in needs.
6. On GitHub, go to repo **Settings → Secrets and variables → Actions → New repository
   secret**:
   - Name: `GOOGLE_SERVICES_JSON`
   - Value: paste the entire contents of `google-services.json`
7. Re-run the **Android APK** workflow (Actions → Android APK → Run workflow) and
   install the new APK.

Accounts created with email get a verification email. "Forgot password?" sends a reset
link.

## First run: statement first

Before reading any SMS, FinTrack asks for last month's bank statement (`ui/SetupScreen.kt`):

1. **Add a statement** (PDF or CSV). It becomes the record for its account and dates.
2. **Sort your spending, payee by payee**, biggest first: accept the suggestion (✓), pick from the
   dropdown, or skip. Each answer covers every payment to that payee (same UPI ID or name), now and
   later (`core/.../Onboarding.kt`).
3. **One more month helps:** with fewer than two months, FinTrack recommends adding another month so
   it learns regular payments (rent, bills, salary). Optional.
4. **Allow SMS reading**: new payments are then sorted the way you taught it.

Setup can be skipped at any step; existing users see it once too.

## Smart categories (on-device machine learning)

Categories are learned on the phone. Nothing is sent anywhere, and no API key is needed.

- **Model:** multinomial Naive Bayes (`core/.../Learner.kt`). It learns from the merchant
  name (words and 3-letter fragments, so "SWIGGY*INSTAMART" resembles "Swiggy"), the amount
  range, the day of the month, and SMS clues such as UPI P2A (to a person) vs P2M (to a merchant).
- **Training data:** the built-in brand list, transactions that list categorized, and every
  category you set yourself (weighted 5×). The model's own guesses are never trained on, so a
  wrong guess can't reinforce itself.
- **Who wins:** your "always use this for X" rules → the built-in brand list → the model
  (only when it's at least 60% sure *and* the name looks like something it learned) → Other.
  Learned categories show a ✦ in Transactions.
- **Daily small spends:** unknown payees of ₹500 or less (tea, snacks, the local shop over UPI) go here
  automatically instead of "Other". The model can still learn better categories for them.
- **Sort "Other" on Home:** payments FinTrack couldn't place are grouped by payee, biggest first; one tap
  per payee sorts all of them.
- **Payee memory (`core/.../PayeeMemory.kt`):** a category you give a payment is reused for (1) the
  same payee, matched by UPI ID or by name however it's written, and (2) the same monthly payment: the
  same amount (±1%) around the same day in a later month, from ₹500 up (e.g. ₹3,200 rent on the 2nd).
  These show ✦ and say why in the picker. Payments to people can be sorted too ("What for?"); sorting
  one as Rent, Fees… makes it spending, and "Just a person" keeps it as money given.
- **Changing a category is one tap:** every transaction shows what it is as a label. Tap the
  transaction and answer "What is this?". It applies to that payee from now on and trains the model.
- **When it retrains:** after every SMS scan and every correction (milliseconds).
- **Insights → Smart categories** shows how many categories you've taught, how many it
  sorted itself, and its accuracy on your own corrections (5-fold cross-validation, once
  you've made 10 or more).

## Money flows: getting the totals right

Every transaction gets a **type** as well as a category (`core/.../Flow.kt`). Only spending
(minus refunds) counts as "spent", and Home shows the month as a statement:
**money in − spent − invested − sent to people = left over**. Money moved between your own
accounts and duplicates are listed separately as *not counted*.

How the type is decided, strongest evidence first:
1. **Your answers:** a type you set on a transaction, or for a payee.
2. **Proof from the data:**
   - *Duplicates:* the same reference twice, or the same amount reported by different senders (bank SMS + UPI app) on the same day. Two identical SMS from the same bank are two real payments.
   - *Statement + SMS:* a statement row for a payment already captured from SMS (same amount and direction within a day, whatever the reference number) counts once; 2–3 days apart, FinTrack asks. Each transaction pairs with at most one other.
   - *Own transfers:* a debit from one of your accounts matched by the same amount arriving in a different one of your accounts within 2 days; an SMS that names another of your accounts ("to A/c XX5678", "towards card XX9012"); card-bill payments (also found in statement narrations).
   - *Wallets:* money counts when it leaves your bank to top up a wallet (Paytm, Amazon Pay, railway R-Wallet). "Added to your wallet" isn't income, and paying from the wallet isn't counted a second time.
   - *Salary at month-end:* a salary credited from the 24th on counts toward the next month, the one it pays for. Salary is a credit worded as salary, or a ₹10,000+ income that recurs at about the same amount.
   - *Interest* (e.g. a savings account's daily interest) is income, even when it comes from your own account's bank.
   - *Your own name:* once you've marked two payees with your name as your own account (e.g. "Mikku Thomas John", "MR - Mikku"), other payments to that name are treated as moving money between your accounts. "Who is it?" questions offer **Shop · Person · Me**.
   - *Refunds and reversals:* money back from a merchant you paid, which reduces that category's spending.
3. **Built-in signals:** UPI to personal IDs (P2A, `@ok…`) means money to or from people; the Investments category means invested.
4. **The flow model:** a second on-device Naive Bayes model learns types from your answers. It only acts when confident, and never guesses own transfers or duplicates.
5. **Questions instead of guesses:** uncertain duplicates, the same amount leaving and arriving when one SMS doesn't name its account ("moved between your own accounts?"), and payees you pay often via personal UPI IDs ("shop or person?") appear in **Needs your input** on Home. One tap answers, fixes the totals and teaches the model.

**Accounts to track.** A phone often gets SMS for accounts that aren't the user's money (a friend's
or family member's). FinTrack lists every account in the messages, by bank and last 4 digits, with
its transaction count and last balance, and asks once (Home, when there are two or more) which to
track; Settings → Accounts to track changes it. Untracked accounts are left out of every total and
list. Messages without an account number are matched to the bank's only account when there is one.

**Every transaction shows what can be done to it.** Tap its category label to change it (a dropdown); tap ⋮ for
the panel with three buttons, **Category · Edit · Delete**. Edit changes the amount, name, date or
money in/out. Delete asks once; a deleted SMS or statement row never comes back on a rescan.

**Dropdowns:** the month (tap its name for the last 12 months), the Transactions filters (Show:
All / Money out / Money in; Category), and "What was this for?" when sorting or adding a transaction.

**Smart sorting (Settings, at the bottom)** shows what the learning did this month: how many payments FinTrack sorted
by itself (✦ learned from your choices, known shops and brands, small daily spends), how many you
sorted, and how many still need a category, each opening that list, plus the model's measured
accuracy on your own choices.

The app has three tabs: **Home · Transactions · Settings**. Charts and checks (pace, daily spending,
trends, unusual spends, bank balance check) are under Settings.

**One question per transaction: "What is this?"** Tap any transaction (or add one with **+**) and
pick from one list: a category (Food, Rent, Bills…) or what kind of money it is (*Gave to a person ·
Investment / savings · My own account · Duplicate · Don't count*; for money in: *Salary / income ·
Refund · From a person · My own account*). It's remembered for the payee. Manual entries ask the same
question, so they appear in their category.

Home reads like a statement: **Money in − Money out = Left**. Money out is everything that left
(spending, people, investments); **Where your money went** lists all of it, so it adds up to Money out.

**Check my numbers (Profile):** totals still look wrong? This shares the month as text: every
transaction, where it came from (SMS, statement, added by hand), what it was counted as and why.
Account numbers, phone numbers and UPI IDs are hidden. It shows at a glance whether the problem is
missing messages or something counted twice.

**Bank balance check (Insights):** bank SMS usually include the balance. For each account,
the balance must move by exactly the transactions captured in between. A gap pinpoints a
missed payment or a duplicate still counted, with dates.

## Bank statements

**Profile → Import bank statements** takes one or more statement files at once: PDFs (including
password-protected ones, where the password is used once and never stored) or CSV exports from
net banking. Everything is read on the phone (`core/.../StatementParser.kt`, PdfBox-Android).

- **Rows:** every line that starts with a date and ends with amounts is a transaction. The last
  amount is the running balance; wrapped narrations are joined back together. Oldest-first and
  newest-first statements both work.
- **Mass balance:** each row must move the running balance by exactly its amount, which also
  settles whether it's money in or out. The whole statement must satisfy
  opening + money in − money out = closing. The report shows **✓ Balanced** or **⚠ Off by ₹X**,
  plus the rows that broke the chain (those are the ones to check).
- **The statement is the record:** every row is imported, tagged with the account read from the
  statement's header (or chosen by you when the header has no account number). For that account and
  the statement's dates, SMS are set aside, so nothing is counted twice and SMS misreads don't matter
  (`core/.../StatementLedger.kt`). Outside those dates, SMS fill in as usual. Re-importing the same
  file adds nothing.
- **Does it tally? (Home):** for each statement with rows in the month: its money out and money in,
  and where FinTrack put every rupee (spent, gave to people, investments, moved to your other
  accounts, left out by you). The lines add up to the statement's totals.
- **Learning:** categories are re-learned after every import, and corrections on statement rows
  teach the model like any other. The model never trains on its own guesses.
- **Not yet supported:** scanned (photo/image) PDFs and Excel files. The app says so instead of
  guessing.

## Charts and learned spending patterns

Each chart answers one question:

| Where | Chart | Question it answers |
|---|---|---|
| Home | **Spent / Money in / Left**: three numbers, each opens its transactions | How did this month go? |
| Home | **Spending by category**: Food, Rent, Bills… with amount, share, number of payments and a bar | Which categories take my money? *Tap one to open it.* |
| Home | **Latest spends**: each with its category label; tap the label to change it | Is it sorted right? |
| Insights | **Spending pace**: this month's running total vs last month (dashed), dotted line to the forecast | Am I spending faster than usual? |
| Insights | **Daily spending**: bar per day, ₹ axis, dashed daily average, dot for today | Which days were heavy? |
| Category page | Total, share, *usual* and month-end forecast; 6-month trend; day by day; top merchants; unusual spends; all its payments | Everything about Food (or any category) |
| Insights | **Month by month** for 6 months with your usual level; **Unusual spends** | Am I improving? What stood out? |

Learned on the phone from your own history (`core/.../Patterns.kt`):
- **Usual month:** the median of past months, overall and per category (needs 2 months of history).
- **Month-end forecast:** uses how much of a month's spending has usually happened by today's
  date, so rent on the 1st doesn't inflate it. Falls back to a straight-line pace after a week
  when there's no history yet.
- **Unusual spends:** at least 2.5× (and ₹500 over) your usual payment in that category, and
  a statistical outlier (median/MAD z-score ≥ 3.5). Needs 5 earlier payments in the category.

Category colours are fixed (Food is always the same blue) and checked for colour-blind
separation; categories are always labelled, never shown by colour alone.

## Build it yourself

You need Android Studio (or JDK 17 and the Android SDK).

```bash
cd android
./gradlew :core:test :app:assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

To get login in a local build, put `google-services.json` in `android/app/`.

## Layout

| Module | What it does |
|---|---|
| `core/` | Pure Kotlin, unit-tested: SMS and statement parsers, categories, summaries, spending patterns, on-device learner |
| `app/data` | SQLite storage on the phone, settings |
| `app/sms` | Inbox scan (history + catch-up when the app opens) and a live SMS receiver |
| `app/statement` | Bank statement import: PDF/CSV text extraction, matching against SMS, insert |
| `app/auth` | Firebase Auth: Google via Credential Manager, email/password |
| `app/ui` | Jetpack Compose UI: `Pop.kt` (NeoPOP-style design system), `Charts.kt` / `ChartsMore.kt` (daily bars, donut, pace line, monthly bars), `HomeScreen`, `CategoryScreen`, `ActivityScreen`, `InsightsScreen`, `ProfileScreen`, `LoginScreen` |

## Design

The look follows the principles of CRED's open-source **NeoPOP** design system, re-implemented in
Jetpack Compose (`app/ui/Pop.kt`); no CRED code or brand assets are used. It has:

- a near-black canvas, flat dark tiles with hairline borders and sharp corners;
- bold uppercase captions and heavy numbers;
- white primary buttons and one violet accent for the screen's key tile;
- **plunk** buttons and tiles with hard 3D edges that press flat when tapped;
- a full-width bottom bar with three destinations (Home, Transactions, Settings);
- always dark, with light system-bar icons.

Category colours are fixed and checked for colour-blind separation on the dark surface.

## Publishing on Google Play

Play restricts SMS permissions, but lists **SMS-based money management** (apps that track and
manage budget) as a permitted exception for READ_SMS / RECEIVE_SMS. Publishing needs a Play
developer account, a privacy policy, and a Permissions Declaration Form explaining that reading
transaction SMS is FinTrack's core function. Google reviews this manually, and approval is not
guaranteed. Until then FinTrack is distributed as an APK.
