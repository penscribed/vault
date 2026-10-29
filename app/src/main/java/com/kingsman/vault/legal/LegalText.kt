package com.kingsman.vault.legal

/**
 * The legal texts shown in the app. The same wording is published as web pages in /docs
 * (GitHub Pages) so there is a public link for app stores.
 *
 * When you change the Privacy Policy or Terms in a way users should re-accept,
 * increase CONSENT_VERSION — everyone will see the consent screen again.
 */
object LegalText {
    const val CONSENT_VERSION = 1
    const val PUBLISHER = "Nnabuike Atuogba"
    const val CONTACT_EMAIL = "nnabuikejoe@gmail.com"
    const val EFFECTIVE_DATE = "29 October 2026"
    const val PRIVACY_URL = "https://penscribed.github.io/vault/privacy.html"
    const val TERMS_URL = "https://penscribed.github.io/vault/terms.html"

    data class Section(val heading: String, val body: String)

    val privacy = listOf(
        Section(
            "Who we are",
            "Vault is a personal and business money-tracking app published by $PUBLISHER (\"we\", \"us\"). " +
                "This policy explains what information Vault handles and how. Effective date: $EFFECTIVE_DATE. " +
                "Contact: $CONTACT_EMAIL."
        ),
        Section(
            "The short version",
            "Vault has no accounts, no servers, no advertising and no analytics. Everything you enter stays on your " +
                "phone, encrypted. We cannot see, collect, sell or share your financial records, because they never reach us."
        ),
        Section(
            "Information stored on your phone",
            "• Income, expenses, categories, notes and tags you enter\n" +
                "• Savings goals, investments, debts, budgets, recurring entries and subscriptions\n" +
                "• Names of businesses and people you record (for example, who owes you money)\n" +
                "• Receipt photos you choose to attach\n" +
                "• Bank alert messages, only if you turn on bank-alert reading (see below)\n" +
                "• Your settings and a scrambled (hashed) form of your PIN — never the PIN itself\n\n" +
                "This information is stored in an encrypted database and encrypted files inside Vault's private storage. " +
                "Other apps cannot read it. Android's cloud backup and device-to-device transfer are switched off for Vault."
        ),
        Section(
            "Bank SMS alerts (optional)",
            "Bank-alert reading is OFF until you switch it on in More › Bank alerts and grant SMS permission. When it is on, " +
                "Vault reads incoming and recent text messages on your phone to find bank debit/credit alerts and turn them into " +
                "suggested entries. Messages that do not look like bank alerts are ignored and never stored. Bank alerts that are " +
                "kept are stored encrypted on your phone only. Vault never sends SMS, never uploads messages and never shares them. " +
                "You can switch this off at any time; you can also remove the permission in Android Settings."
        ),
        Section(
            "Camera and photos (optional)",
            "Used only when you choose to attach a receipt photo. Photos are shrunk, their location and camera details are removed, " +
                "and they are encrypted inside Vault. Vault does not access your gallery except for the photo you pick."
        ),
        Section(
            "Fingerprint unlock (optional)",
            "Fingerprint checks are performed by Android. Vault only receives a yes/no answer and never receives or stores " +
                "fingerprint data."
        ),
        Section(
            "Notifications",
            "Used for budget alerts, new bank alerts to review and app updates. Details are hidden on the lock screen. " +
                "You can turn notifications off in Android Settings."
        ),
        Section(
            "Internet use",
            "Vault connects to the internet only to check GitHub (github.com) for new versions of the app and to download them. " +
                "No personal or financial information is sent. As with any website, GitHub can see your device's IP address and " +
                "that a request was made; GitHub's own privacy statement applies to that. You can turn automatic update checks off " +
                "in More › App updates."
        ),
        Section(
            "Backups, reports and sharing",
            "Backups and PDF reports are created only when you ask. Backups are encrypted with a password you choose. " +
                "When you send a backup or report by email or another app, that app or provider handles it under its own " +
                "privacy policy. Keep your backup password safe — we cannot recover it."
        ),
        Section(
            "What we do not do",
            "We do not collect personal data, track you, show ads, sell or share data with third parties, or use your data to " +
                "train any system. There are no third-party analytics or crash-reporting tools in Vault."
        ),
        Section(
            "How long data is kept",
            "Your data stays on your phone until you delete it. You can delete everything at any time in " +
                "More › Privacy & legal › Delete all my data, or by uninstalling Vault. Deletion cannot be undone " +
                "unless you have a backup."
        ),
        Section(
            "Your rights",
            "Depending on where you live (for example under Nigeria's Data Protection Act 2023, the UK GDPR or the EU GDPR), " +
                "you have rights to access, correct, export and delete your personal data and to withdraw consent. Because your " +
                "data is held only on your device, you can exercise these rights directly in the app: view and edit any record, " +
                "export with Backup or PDF reports, withdraw SMS/camera/notification permissions, and delete all data. " +
                "For any question or complaint, contact $CONTACT_EMAIL. You may also contact your local data-protection " +
                "authority (in Nigeria, the Nigeria Data Protection Commission)."
        ),
        Section(
            "Children",
            "Vault is intended for adults managing their own or their business's money. It is not directed at children."
        ),
        Section(
            "Security",
            "Vault uses AES-256 encryption, keys protected by the Android Keystore, PIN and fingerprint lock, automatic lock, " +
                "screenshot blocking and optional erase after repeated wrong PINs. No method is perfect; keep your phone, PIN " +
                "and backup password secure."
        ),
        Section(
            "Changes to this policy",
            "If this policy changes, the updated version will appear in the app and at $PRIVACY_URL. Significant changes will be " +
                "shown to you in the app before you continue using it."
        ),
        Section("Contact", "$PUBLISHER — $CONTACT_EMAIL")
    )

    val terms = listOf(
        Section(
            "Agreement",
            "By using Vault you agree to these Terms. If you do not agree, do not use the app. Effective date: $EFFECTIVE_DATE. " +
                "Publisher: $PUBLISHER ($CONTACT_EMAIL)."
        ),
        Section(
            "What Vault is",
            "Vault is a record-keeping tool for tracking income, expenses, savings, investments, debts and budgets. " +
                "It is not a bank, payment service, lender, investment service or accounting firm, and it does not move money."
        ),
        Section(
            "Not professional advice",
            "Nothing in Vault is financial, investment, tax, legal or accounting advice. Totals, reports, budget alerts and " +
                "suggestions are based only on what you enter or allow the app to read, and may be incomplete or wrong — for " +
                "example if a bank alert is read incorrectly. Check important figures against your bank statements and consult " +
                "a qualified professional before making financial, tax or business decisions."
        ),
        Section(
            "Your responsibilities",
            "You are responsible for the accuracy of what you record, for keeping your phone, PIN and backup password secure, " +
                "for making backups, and for using Vault lawfully — including any obligations you have when recording information " +
                "about other people or businesses."
        ),
        Section(
            "Data loss",
            "Your data is stored only on your device. If you lose your phone, forget your PIN, enable erase-after-wrong-PINs, " +
                "delete the app or lose your backup password, your data may be lost permanently. We cannot recover it."
        ),
        Section(
            "Updates",
            "Vault may offer updates downloaded from GitHub. Updates are optional and installed only after you confirm them."
        ),
        Section(
            "Provided \"as is\"",
            "Vault is provided free of charge and \"as is\", without warranties of any kind, to the fullest extent permitted by law."
        ),
        Section(
            "Limitation of liability",
            "To the fullest extent permitted by law, $PUBLISHER is not liable for any indirect, incidental or consequential loss, " +
                "or for any loss of data, money, profit or business arising from use of, or inability to use, Vault. Nothing in " +
                "these Terms limits liability that cannot be limited by law, or your statutory consumer rights."
        ),
        Section(
            "Open-source software",
            "Vault includes open-source components used under their own licences. See More › Privacy & legal › Open-source licences."
        ),
        Section(
            "Changes and governing law",
            "These Terms may be updated; the current version is always in the app and at $TERMS_URL. These Terms are governed by " +
                "the laws of the Federal Republic of Nigeria, without affecting any mandatory consumer protections of the country " +
                "where you live."
        ),
        Section("Contact", "$PUBLISHER — $CONTACT_EMAIL")
    )

    val licences = listOf(
        Section(
            "Apache License 2.0",
            "The following are used under the Apache License, Version 2.0:\n" +
                "• Android Jetpack / AndroidX (Compose, Material 3, Room, WorkManager, Navigation, Lifecycle, Biometric, " +
                "Core, Activity, Fragment, ExifInterface, SQLite) — Copyright The Android Open Source Project\n" +
                "• Material Icons — Copyright Google LLC\n" +
                "• Kotlin and kotlinx.coroutines — Copyright JetBrains s.r.o. and Kotlin contributors\n" +
                "• Gson — Copyright Google Inc.\n\n" +
                "Licensed under the Apache License, Version 2.0 (the \"License\"); you may not use these files except in " +
                "compliance with the License. You may obtain a copy of the License at " +
                "https://www.apache.org/licenses/LICENSE-2.0. Unless required by applicable law or agreed to in writing, " +
                "software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS " +
                "OF ANY KIND, either express or implied. See the License for the specific language governing permissions and " +
                "limitations under the License."
        ),
        Section(
            "SQLCipher for Android",
            "Copyright (c) Zetetic LLC. All rights reserved.\n\n" +
                "Redistribution and use in source and binary forms, with or without modification, are permitted provided that " +
                "the following conditions are met: (1) Redistributions of source code must retain the above copyright notice, " +
                "this list of conditions and the following disclaimer. (2) Redistributions in binary form must reproduce the " +
                "above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other " +
                "materials provided with the distribution. (3) Neither the name of the ZETETIC LLC nor the names of its " +
                "contributors may be used to endorse or promote products derived from this software without specific prior " +
                "written permission.\n\n" +
                "THIS SOFTWARE IS PROVIDED BY ZETETIC LLC ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT " +
                "LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO " +
                "EVENT SHALL ZETETIC LLC BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL " +
                "DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR " +
                "PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT " +
                "LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN " +
                "IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
        ),
        Section("SQLite", "SQLite is in the public domain. https://www.sqlite.org/copyright.html"),
        Section("OpenSSL / crypto components", "Cryptographic components bundled with SQLCipher are used under their respective open-source licences.")
    )

    /** Shown before Android's SMS permission prompt (Google Play "prominent disclosure"). */
    const val SMS_DISCLOSURE =
        "Vault will read text messages on this phone to find bank debit and credit alerts, including messages from the " +
            "last 30 days and new ones as they arrive, and turn them into suggested entries for you to review.\n\n" +
            "• Only messages that look like bank alerts are kept, encrypted on this phone.\n" +
            "• All other messages are ignored and never stored.\n" +
            "• Nothing is uploaded or shared, and Vault never sends texts.\n" +
            "• You can switch this off any time in More › Bank alerts.\n\n" +
            "Do you want to allow this?"
}
