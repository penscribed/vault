package com.kingsman.vault.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kingsman.vault.R
import com.kingsman.vault.legal.LegalText
import com.kingsman.vault.ui.PrimaryButton
import com.kingsman.vault.ui.ScreenHeader
import com.kingsman.vault.ui.SecondaryButton
import com.kingsman.vault.ui.VCard
import com.kingsman.vault.ui.theme.LocalVault

object LegalDoc {
    const val PRIVACY = "privacy"
    const val TERMS = "terms"
    const val LICENCES = "licences"

    fun title(doc: String) = when (doc) {
        PRIVACY -> "Privacy Policy"
        TERMS -> "Terms of Use"
        else -> "Open-source licences"
    }

    fun sections(doc: String) = when (doc) {
        PRIVACY -> LegalText.privacy
        TERMS -> LegalText.terms
        else -> LegalText.licences
    }
}

/** The body of a legal document, as a list of headed sections. */
@Composable
fun LegalBody(doc: String, modifier: Modifier = Modifier) {
    val c = LocalVault.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (doc != LegalDoc.LICENCES) {
            Text(
                "Effective ${LegalText.EFFECTIVE_DATE} · ${LegalText.PUBLISHER}",
                color = c.muted, fontSize = 12.sp
            )
        }
        LegalDoc.sections(doc).forEach { s ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(s.heading, color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(s.body, color = c.soft, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        if (doc != LegalDoc.LICENCES) {
            Text(
                "Also online: " + if (doc == LegalDoc.PRIVACY) LegalText.PRIVACY_URL else LegalText.TERMS_URL,
                color = c.muted, fontSize = 12.sp
            )
        }
    }
}

/** Full screen (reached from More › Privacy & legal). */
@Composable
fun LegalScreen(doc: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(LegalDoc.title(doc), onBack)
        LegalBody(
            doc,
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        )
    }
}

/** Full-screen reader used from the consent screen (before the app is set up). */
@Composable
fun LegalDialog(doc: String, onDismiss: () -> Unit) {
    val c = LocalVault.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(c.bg)
        ) {
            ScreenHeader(LegalDoc.title(doc), onDismiss)
            LegalBody(
                doc,
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
            PrimaryButton("Done", Modifier.fillMaxWidth().padding(20.dp)) { onDismiss() }
        }
    }
}

/** Shown once on first launch (and again if the policy version changes), before the PIN is created. */
@Composable
fun ConsentScreen(onAccept: () -> Unit) {
    val c = LocalVault.current
    var agreed by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.vault_logo), contentDescription = "Vault logo",
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Welcome to Vault", color = c.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Track • Plan • Grow", color = c.muted, fontSize = 13.sp)
            }
        }

        VCard(Modifier.fillMaxWidth()) {
            Text("Your money stays on your phone", color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "• No account, no ads, no tracking.\n" +
                    "• Everything you record is encrypted on this device.\n" +
                    "• Bank SMS reading, camera and notifications are optional and only used if you turn them on.\n" +
                    "• Vault only goes online to check GitHub for app updates — none of your data is sent.\n" +
                    "• You can delete all your data at any time.",
                color = c.soft, fontSize = 14.sp, lineHeight = 21.sp
            )
        }

        VCard(Modifier.fillMaxWidth()) {
            Text("Important", color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Vault is a record-keeping tool, not financial, tax or accounting advice. If you lose your phone, PIN or backup " +
                    "password, your data cannot be recovered — make regular backups.",
                color = c.soft, fontSize = 14.sp, lineHeight = 21.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Privacy Policy", Modifier.weight(1f)) { reading = LegalDoc.PRIVACY }
            SecondaryButton("Terms of Use", Modifier.weight(1f)) { reading = LegalDoc.TERMS }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Checkbox) { agreed = !agreed }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = agreed, onCheckedChange = { agreed = it },
                colors = CheckboxDefaults.colors(checkedColor = c.acc, checkmarkColor = c.accInk, uncheckedColor = c.muted)
            )
            Text(
                "I have read and agree to the Privacy Policy and Terms of Use, and I am 18 or older.",
                color = c.text, fontSize = 14.sp
            )
        }

        PrimaryButton("Agree and continue", Modifier.fillMaxWidth(), enabled = agreed) { onAccept() }
        Text(
            "Published by ${LegalText.PUBLISHER} · ${LegalText.CONTACT_EMAIL}",
            color = c.muted, fontSize = 12.sp
        )
    }

    reading?.let { LegalDialog(it) { reading = null } }
}

/** Two-step confirmation before erasing everything. */
@Composable
fun DeleteAllDataDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val c = LocalVault.current
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete all my data?", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "This permanently erases every entry, business, goal, investment, debt, budget, receipt photo, bank alert, " +
                        "your PIN and all settings from this phone. Backups you already emailed are not affected.\n\n" +
                        "This cannot be undone. Type DELETE to confirm."
                )
                OutlinedTextField(
                    value = typed, onValueChange = { typed = it }, singleLine = true,
                    placeholder = { Text("DELETE") }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = typed.trim() == "DELETE", onClick = onConfirm) {
                Text("Delete everything", color = if (typed.trim() == "DELETE") c.exp else c.muted)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
