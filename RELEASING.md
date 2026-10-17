# Putting Vault on GitHub and shipping updates

You do steps 1–6 once. After that, every update is step 7.

---

## 1. Create the GitHub repository

1. Go to **github.com/new** (signed in as `alpho4jesus-dev`).
2. **Repository name:** `vault`
3. Choose **Public**. The app reads releases without a password, so the repo has to be public. No passwords or keys are in the code.
4. Leave **Add a README**, **.gitignore** and **license** unticked. The repo must be empty.
5. Click **Create repository**.

## 2. Upload the project

**Easiest — from Android Studio**
1. Unzip `Vault.zip` and open the `Vault` folder in Android Studio.
2. Menu **VCS → Enable Version Control Integration → Git** (skip if it's already a Git project).
3. Menu **Git → Commit…** → tick all files → message `Vault 1.2.0` → **Commit**.
4. Menu **Git → Manage Remotes… → +** → URL `https://github.com/alpho4jesus-dev/vault.git` → OK.
5. Menu **Git → Push…** → **Push**. Sign in to GitHub when asked.

**Or from a terminal / Command Prompt inside the `Vault` folder**
```
git init
git add .
git commit -m "Vault 1.2.0"
git branch -M main
git remote add origin https://github.com/alpho4jesus-dev/vault.git
git push -u origin main
```

## 3. Create your signing key (once, ever)

Every Vault update must be signed with the **same key**, or phones refuse it.

1. In Android Studio: **Build → Generate Signed App Bundle / APK… → APK → Next → Create new…**
2. **Key store path:** save it **outside** the project folder, e.g. `Documents\Keys\vault-release.jks`.
3. Choose a strong **password**, **alias** `vault`, a **key password** (can be the same), validity **100** years, and your name in *First and Last Name*.
4. Click **OK**, then **Cancel** (you don't need to build here).
5. **Back up `vault-release.jks` and both passwords** somewhere safe, e.g. a password manager or an encrypted drive.
   If you lose them, you can never update installed copies again.

## 4. Turn the key into text for GitHub

**Windows (PowerShell):**
```
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\Documents\Keys\vault-release.jks")) | Set-Clipboard
```
**Mac:**
```
base64 -i ~/Documents/Keys/vault-release.jks | pbcopy
```
The long text is now on your clipboard.

## 5. Add four secrets to the repo

On GitHub go to **vault → Settings → Secrets and variables → Actions → New repository secret** and add:

| Name | Value |
|---|---|
| `KEYSTORE_BASE64` | paste the clipboard from step 4 |
| `SIGNING_STORE_PASSWORD` | the key store password |
| `SIGNING_KEY_ALIAS` | `vault` |
| `SIGNING_KEY_PASSWORD` | the key password |

GitHub keeps these encrypted. Nobody, including people who can see the code, can read them.

## 6. Check that it builds

Open the **Actions** tab. **Build check** runs automatically after your push.
- ✅ Green: everything compiles.
- ❌ Red: open it, copy the red error lines, and send them to Claude to fix.

## 7. Publish a release (first version and every update)

1. **Actions → Release → Run workflow**.
2. **Version:** `1.2.0` the first time. Each later release must be higher: `1.2.1`, `1.3.0`, …
3. Click **Run workflow**. After about 5–8 minutes, a release appears under **Releases** with `Vault-1.2.0.apk`.

*(Alternative: push a git tag such as `v1.2.1`. That triggers the same thing.)*

## 8. Install on your phone (first time only)

1. On the phone, open **github.com/alpho4jesus-dev/vault/releases**.
2. Download `Vault-1.2.0.apk` and open it. If Android asks, allow installs from your browser.
3. **If you already had Vault installed from Android Studio:**
   in the old app go to **More › Backup & restore** and email yourself a backup. Then uninstall it,
   install the GitHub APK, and choose **Restore** with that file.
   (The Android Studio copy uses a different key, so it can't be updated in place. You only do this once.)

---

## How updates reach the phone after that

- Vault checks GitHub once a day and whenever you unlock it.
- A **"Vault x.y.z is available"** card appears on Home, and you get a notification.
- Tap it, then **Download & install**. Vault verifies the file's checksum, and Android shows the install screen.
- The first time, Android asks you to **allow installs from Vault**. Turn it on and come back.
- Your records, PIN and settings stay exactly as they are.
- You can check manually under **More › App updates › Check now**.

## Building a signed APK on your own computer (optional)

Create `keystore.properties` in the `Vault` folder. It is ignored by Git and never uploaded.
```
storeFile=C:/Users/<you>/Documents/Keys/vault-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=vault
keyPassword=YOUR_KEY_PASSWORD
```
Then run **Build → Build App Bundle(s) / APK(s) → Build APK(s)** with the *release* build variant.
