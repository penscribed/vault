# Vault — Track • Plan • Grow

Offline personal **and** business money tracker for Android. Everything stays on the phone, encrypted.

## Open & run (Android Studio)

1. Unzip, then **File › Open** and select the `Vault` folder (the one containing `settings.gradle.kts`).
2. Let Gradle sync (first sync downloads Gradle 8.7 and the libraries — needs internet once).
   If Studio offers to upgrade the Android Gradle Plugin, you can accept.
3. Plug in your phone (USB debugging on) or start an emulator, then press **Run ▶**.
4. First launch: create a 4–6 digit PIN. Turn on fingerprint in **More › Security**.

To build an installable APK: **Build › Build App Bundle(s) / APK(s) › Build APK(s)**.
For the Play Store later: **Build › Generate Signed App Bundle** (keep your keystore file safe).

Requirements: Android 8.0+ (minSdk 26), JDK 17 (bundled with Android Studio).

## What's inside

| Area | Features |
|---|---|
| **Modes** | Personal / Business toggle (amber vs blue theme). Unlimited businesses, each with separate books. |
| **Entries** | 5-second add screen with number pad, category tiles sorted by how often you use them, date, note, tag, "Save + new". Any entry can be moved between Personal and a business. |
| **Currencies** | Every ISO currency, searchable. The last currency you pick becomes the default. Totals are kept per currency (no conversion). |
| **Business money in personal account** | Mark a business expense "Paid from my personal money" — Home shows what your businesses owe you until you mark it reimbursed. |
| **Recurring & subscriptions** | Weekly / monthly / quarterly / yearly. Fixed amounts log themselves. Variable amounts (commission) show a "due — enter amount" card on Home. Subscriptions screen shows monthly cost. |
| **Savings goals** | Custom names, target, optional date, deposits & withdrawals, progress. |
| **Investments** | Type, money put in, current value, gain. |
| **Debts** | "They owe me" and "I owe" (no interest), partial payments, due dates, settle/reopen. |
| **Reports** | This month / last month / this year / custom range, by currency: income, expenses, net, % kept, category breakdowns, month-by-month, wealth snapshot, open debts. **Export to PDF** and share. |
| **Backup** | One encrypted `.vault` file (AES-256, your password) shared to email via the share sheet. Restore from the file. Reminder when your last backup is over 30 days old. |
| **Security** | PIN (salted PBKDF2 hash) + fingerprint, auto-lock (immediately / 30s / 1 min / 5 min), lockout after 5 wrong PINs, optional erase after 10, encrypted database (SQLCipher, key in Android Keystore), screenshots & recents preview blocked, Android cloud/device backup disabled, hide-amounts eye toggle. |
| **Bank alerts → entries** | Turn on *More › Bank alerts* and Vault reads debit/credit SMS from any bank or wallet (GTBank, Access, Zenith, UBA, First Bank, Opay, Kuda, Moniepoint, Palmpay, US/UK banks…) and lists them for one-tap adding, with a notification for new ones. Works on alerts that are already in your inbox (last 30 days) too. No SMS permission? Paste an alert, or long-press it in any app → **Share → Vault**. Only bank-like messages are kept (encrypted); nothing is uploaded. |
| **Budgets with alerts** | Monthly limits per category or for all spending, per currency, in Personal or any business. Alert at 50/75/80/90% and again when over — as a notification even when the app is closed (details hidden on the lock screen). Daily allowance for the rest of the month. Top 3 shown on Home. |
| **Receipt photos** | Snap or pick a photo on any entry. Photos are shrunk, stripped of location data and **encrypted** in private storage. View from Records. Optional in backups. |
| **Look** | Midnight design, dark ↔ light ↔ system, your Vault logo as app icon. |

## Auto-updates from GitHub

Vault checks **github.com/penscribed/vault** releases daily and offers new versions in-app
(download → checksum check → Android's install screen; data is kept).
**Setup and how to publish a release: see `RELEASING.md`.**

## Note on SMS reading and the Play Store

Google Play only allows `READ_SMS`/`RECEIVE_SMS` for a few app types and reviews every request. Sideloaded (your own APK) it works fully.
Before publishing, either apply for the SMS permission exception in Play Console, or remove the two SMS permissions and the
`SmsReceiver` from `AndroidManifest.xml` — paste and **Share → Vault** keep working without them.

## Updating from 1.0

The database upgrades itself (version 1 → 2) and keeps all existing entries.

## Project layout

```
app/src/main/java/com/kingsman/vault/
  VaultApp.kt            app singleton, encrypted DB, wipe
  MainActivity.kt        theme, auto-lock, fingerprint, sharing
  data/                  Room entities, DAO, database, repository (recurring, backup), settings
  security/              PIN hashing, DB key, backup encryption
  util/                  money formatting, dates, calculations, report builder, PDF writer
  ui/                    navigation, shared components, view model, theme
  ui/screens/            Lock, Home, Add entry, Records, Wealth, Reports, More (+ sub-screens)
```

Package / application id: `com.kingsman.vault` — change it in `app/build.gradle.kts` before publishing if you want a different one.

## Fonts (optional)

The app uses the system font. To match the mock-ups exactly, download **Manrope** and **Space Grotesk** from Google Fonts,
put the `.ttf` files in `app/src/main/res/font/`, and point `NumberFont` in `ui/theme/Theme.kt` at them.
