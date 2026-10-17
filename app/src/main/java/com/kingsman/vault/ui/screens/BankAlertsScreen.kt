package com.kingsman.vault.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kingsman.vault.MainActivity
import com.kingsman.vault.data.Kind
import com.kingsman.vault.ui.AmountText
import com.kingsman.vault.ui.PrimaryButton
import com.kingsman.vault.ui.Routes
import com.kingsman.vault.ui.ScreenHeader
import com.kingsman.vault.ui.SecondaryButton
import com.kingsman.vault.ui.SectionLabel
import com.kingsman.vault.ui.SettingSwitch
import com.kingsman.vault.ui.VCard
import com.kingsman.vault.ui.VChip
import com.kingsman.vault.ui.VaultViewModel
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.ui.toast
import com.kingsman.vault.util.Dates

@Composable
fun BankAlertsScreen(vm: VaultViewModel, activity: MainActivity, nav: NavHostController, onBack: () -> Unit) {
    val c = LocalVault.current
    val ctx = LocalContext.current
    val prefs = vm.prefs
    val enabled by prefs.smsEnabled.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    var paste by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    val hasPerm = vm.hasSmsPermission()

    fun scan(days: Int) {
        scanning = true
        vm.scanSms(days) { n ->
            scanning = false
            ctx.toast(if (n == 0) "No new bank alerts found" else "Found $n new bank alert${if (n == 1) "" else "s"}")
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Bank alerts", onBack) {
            if (suggestions.isNotEmpty()) {
                TextButton(onClick = { vm.dismissAllSuggestions() }) { Text("Clear all", color = c.muted) }
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ----- To review -----
            if (suggestions.isNotEmpty()) {
                SectionLabel("To review (${suggestions.size})")
                suggestions.forEach { sg ->
                    val income = sg.kind == Kind.INCOME
                    VCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    sg.description, color = c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${if (income) "Money in" else "Money out"} · ${sg.sender} · ${Dates.dayLabel(sg.date)}",
                                    color = c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            AmountText(
                                sg.amount, sg.currency, size = 15, prefix = if (income) "+" else "−",
                                color = if (income) c.inc else c.text
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VChip("Dismiss", modifier = Modifier.weight(1f)) { vm.dismissSuggestion(sg) }
                            VChip("Add to Vault", selected = true, modifier = Modifier.weight(2f)) {
                                nav.navigate(Routes.add(suggestionId = sg.id))
                            }
                        }
                    }
                }
            } else {
                VCard(Modifier.fillMaxWidth()) {
                    Text("Nothing to review", color = c.text, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Bank debit and credit alerts will appear here, ready to add in one tap.",
                        color = c.muted, fontSize = 13.sp
                    )
                }
            }

            // ----- Automatic SMS reading -----
            VCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                SettingSwitch(
                    "Read bank SMS automatically",
                    "Turns new debit/credit alerts into suggestions",
                    enabled && hasPerm
                ) { on ->
                    if (on) {
                        activity.askPermissions(listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)) { ok ->
                            if (ok) {
                                prefs.setSmsEnabled(true)
                                activity.ensureNotificationPermission()
                                scan(30)
                            } else {
                                ctx.toast("SMS permission was not given")
                            }
                        }
                    } else {
                        prefs.setSmsEnabled(false)
                    }
                }
                Text(
                    "Vault only keeps messages that look like bank alerts (an amount plus debit/credit and an account or card). " +
                        "They're stored encrypted on this phone. Other SMS are never stored, and nothing is ever uploaded.",
                    color = c.muted, fontSize = 12.sp
                )
                if (enabled && hasPerm) {
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton(if (scanning) "Scanning…" else "Scan the last 30 days", Modifier.fillMaxWidth()) {
                        if (!scanning) scan(30)
                    }
                }
            }

            // ----- Paste / share -----
            VCard(Modifier.fillMaxWidth()) {
                Text("Paste a bank alert", color = c.text, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Works for SMS, WhatsApp or app notifications. Or long-press an alert in your SMS app → Share → Vault.",
                    color = c.muted, fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = paste, onValueChange = { paste = it },
                    placeholder = { Text("e.g. Acct:017**123 DR:NGN25,000.00 Desc:POS SHOPRITE…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                )
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Read alert", Modifier.fillMaxWidth(), enabled = paste.isNotBlank()) {
                    vm.addPastedAlert(paste) { ok ->
                        if (ok) {
                            paste = ""
                            ctx.toast("Added to the review list")
                        } else {
                            ctx.toast("Couldn't find a debit/credit amount in that text")
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
