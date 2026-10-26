# FinTrack for Android

The Android app reads your bank transaction SMS on the phone and shows where your money
goes: daily spend, categories, top merchants and month-over-month change. You can sign in
with Google or with an email and password. Claude can optionally sort unknown merchants
and write a monthly analysis.

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

## AI features (optional)

In the app, go to **Settings → AI** and paste a Claude API key from
https://console.anthropic.com.
- Merchants the built-in rules can't place get categorized automatically. Only merchant
  names are sent.
- **AI insights** sends only that month's totals by category and merchant, never your SMS.

The key is stored only on your phone. Anyone with the phone unlocked could use it, so
set a spending limit on the key in the Anthropic console.

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
| `core/` | Pure Kotlin, unit-tested: SMS parser, categories, monthly summary, Claude client |
| `app/data` | SQLite storage on the phone, settings |
| `app/sms` | Inbox scan (history + catch-up when the app opens) and a live SMS receiver |
| `app/auth` | Firebase Auth: Google via Credential Manager, email/password |
| `app/ui` | Jetpack Compose UI: `Glass.kt` (design system: glossy backdrop, frosted cards, gradient hero, floating nav), `HomeScreen`, `ActivityScreen`, `InsightsScreen`, `ProfileScreen`, `LoginScreen` |

## Design

Glossy and minimal: a soft colour-washed backdrop, frosted-glass cards with a light rim, and a
blue-to-violet gradient for the one thing that matters on each screen. Navigation is a floating
pill with four destinations (Home, Activity, Insights, Profile). Edits and new entries open as
bottom sheets, so you never lose your place. Light and dark themes follow the phone's setting.

## Publishing on Google Play

Play restricts SMS permissions, but lists **SMS-based money management** (apps that track and
manage budget) as a permitted exception for READ_SMS / RECEIVE_SMS. Publishing needs a Play
developer account, a privacy policy, and a Permissions Declaration Form explaining that reading
transaction SMS is FinTrack's core function. Google reviews this manually, and approval is not
guaranteed. Until then FinTrack is distributed as an APK.
