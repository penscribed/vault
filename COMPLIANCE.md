# Vault — privacy & compliance notes

Publisher: **Nnabuike Atuogba** · Contact: **nnabuikejoe@gmail.com**
Not legal advice — have a lawyer review before wide distribution.

## Built into the app (v1.3.0)
- **First-launch consent**: Privacy Policy + Terms must be accepted (and 18+ confirmed) before setup.
  Raise `CONSENT_VERSION` in `legal/LegalText.kt` when the policy changes to ask everyone again.
- **Privacy Policy, Terms of Use, Open-source licences**: More › Privacy & legal.
- **Public policy links** (GitHub Pages, from the `docs/` folder):
  - https://penscribed.github.io/vault/privacy.html
  - https://penscribed.github.io/vault/terms.html
- **SMS prominent disclosure** shown before Android's SMS permission prompt.
- **Delete all my data** (More › Privacy & legal) — erases database, receipts, keys, PIN and settings.
- **Contact** link (opens email).
- Not-financial-advice disclaimer (consent screen + Terms).

To edit the wording: change `app/src/main/java/com/kingsman/vault/legal/LegalText.kt`
and the matching pages in `docs/`.

## Turn on the public policy pages (once)
GitHub repo → **Settings → Pages** → Source: **Deploy from a branch** → Branch: **main**, folder **/docs** → **Save**.
After ~1 minute the links above work.

## If you publish on Google Play — Data safety form answers
- Does the app collect or share user data? **No** (all data stays on device; nothing is transmitted to the developer or third parties).
- Is data encrypted in transit? **Not applicable / no data transmitted** (update checks contain no user data).
- Can users request deletion? **Yes** — in-app "Delete all my data" or uninstall.
- Privacy policy URL: https://penscribed.github.io/vault/privacy.html
- **SMS permissions**: Play only allows READ_SMS/RECEIVE_SMS for approved use cases. Either submit the
  Permissions Declaration (category is unlikely to qualify) or ship a Play build without the two SMS
  permissions and `SmsReceiver` — paste/Share → Vault still work.
- **REQUEST_INSTALL_PACKAGES** (self-update): Play does not allow apps to update themselves outside Play.
  For a Play build, remove this permission and the GitHub updater; Play handles updates.
- Content rating questionnaire: finance/utility, no user-generated content shared, no ads.

## Nigeria (NDPA 2023) and UK/EU GDPR
Because Vault processes data only on the user's device and the publisher receives none, most controller
obligations (registration, breach reporting, DPO) are unlikely to apply — but this depends on facts and
should be confirmed with a lawyer if you start collecting any data (accounts, cloud sync, analytics, crash reports).
If you ever add any of those, update the Privacy Policy *before* releasing.
