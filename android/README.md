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
transaction is added. Cash spends can be added with the **+** button in Activity.

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
  Learned categories show a ✦ in Activity.
- **When it retrains:** after every SMS scan and every correction (milliseconds).
- **Insights → Smart categories** shows how many categories you've taught, how many it
  sorted itself, and its accuracy on your own corrections (5-fold cross-validation, once
  you've made 10 or more).

## Charts and learned spending patterns

Each chart answers one question:

| Where | Chart | Question it answers |
|---|---|---|
| Home | **Spending pace**: this month's running total vs last month (dashed), dotted line to the forecast | Am I spending faster than usual? |
| Home | **Where it went**: donut plus a labelled list | Which categories take my money? *Tap one to open it.* |
| Home | **Daily spending**: bar per day, ₹ axis, dashed daily average, dot for today | Which days were heavy? |
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
| `core/` | Pure Kotlin, unit-tested: SMS parser, categories, monthly summary, on-device category learner |
| `app/data` | SQLite storage on the phone, settings |
| `app/sms` | Inbox scan (history + catch-up when the app opens) and a live SMS receiver |
| `app/auth` | Firebase Auth: Google via Credential Manager, email/password |
| `app/ui` | Jetpack Compose UI: `Pop.kt` (NeoPOP-style design system), `Charts.kt` / `ChartsMore.kt` (daily bars, donut, pace line, monthly bars), `HomeScreen`, `CategoryScreen`, `ActivityScreen`, `InsightsScreen`, `ProfileScreen`, `LoginScreen` |

## Design

The look follows the principles of CRED's open-source **NeoPOP** design system, re-implemented in
Jetpack Compose (`app/ui/Pop.kt`); no CRED code or brand assets are used. It has:

- a near-black canvas, flat dark tiles with hairline borders and sharp corners;
- bold uppercase captions and heavy numbers;
- white primary buttons and one violet accent for the screen's key tile;
- **plunk** buttons and tiles with hard 3D edges that press flat when tapped;
- a full-width bottom bar with four destinations (Home, Activity, Insights, Profile);
- always dark, with light system-bar icons.

Category colours are fixed and checked for colour-blind separation on the dark surface.

## Publishing on Google Play

Play restricts SMS permissions, but lists **SMS-based money management** (apps that track and
manage budget) as a permitted exception for READ_SMS / RECEIVE_SMS. Publishing needs a Play
developer account, a privacy policy, and a Permissions Declaration Form explaining that reading
transaction SMS is FinTrack's core function. Google reviews this manually, and approval is not
guaranteed. Until then FinTrack is distributed as an APK.
