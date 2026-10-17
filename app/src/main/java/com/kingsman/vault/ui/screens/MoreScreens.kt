package com.kingsman.vault.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kingsman.vault.MainActivity
import com.kingsman.vault.VaultApp
import com.kingsman.vault.data.Business
import com.kingsman.vault.data.Category
import com.kingsman.vault.data.Frequency
import com.kingsman.vault.data.Kind
import com.kingsman.vault.data.Recurring
import com.kingsman.vault.data.Scope
import com.kingsman.vault.ui.AmountText
import com.kingsman.vault.ui.ConfirmDialog
import com.kingsman.vault.ui.CurrencyPickerDialog
import com.kingsman.vault.ui.DatePickDialog
import com.kingsman.vault.ui.EmptyNote
import com.kingsman.vault.ui.Field
import com.kingsman.vault.ui.NO_BIZ
import com.kingsman.vault.ui.NavRow
import com.kingsman.vault.ui.PrimaryButton
import com.kingsman.vault.ui.Routes
import com.kingsman.vault.ui.ScreenHeader
import com.kingsman.vault.ui.SecondaryButton
import com.kingsman.vault.ui.SectionLabel
import com.kingsman.vault.ui.Segmented
import com.kingsman.vault.ui.SettingSwitch
import com.kingsman.vault.ui.TextInputDialog
import com.kingsman.vault.ui.VCard
import com.kingsman.vault.ui.VChip
import com.kingsman.vault.ui.VaultViewModel
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.ui.toast
import com.kingsman.vault.util.Calc
import com.kingsman.vault.util.Dates
import com.kingsman.vault.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ============================== More ==============================

@Composable
fun MoreScreen(vm: VaultViewModel, nav: NavHostController, activity: MainActivity) {
    val c = LocalVault.current
    val prefs = vm.prefs
    val theme by prefs.theme.collectAsState()
    val cur by prefs.defaultCurrency.collectAsState()
    val lastBackup by prefs.lastBackup.collectAsState()
    var pickCur by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("More")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val stale = lastBackup == 0L || System.currentTimeMillis() - lastBackup > 30L * 24 * 3600 * 1000
            if (stale) {
                VCard(Modifier.fillMaxWidth(), bg = c.accSoft, border = c.accLine, onClick = { nav.navigate(Routes.BACKUP) }) {
                    Text("Back up your data", color = c.text, fontWeight = FontWeight.ExtraBold)
                    Text(
                        if (lastBackup == 0L) "You haven't made a backup yet. If you lose this phone, your records go with it."
                        else "Your last backup was ${Dates.fmt(lastBackup)}.",
                        color = c.soft, fontSize = 13.sp
                    )
                }
            }

            SectionLabel("Appearance")
            Segmented(
                listOf("Dark", "Light", "System"),
                when (theme) { "LIGHT" -> 1; "SYSTEM" -> 2; else -> 0 }
            ) { prefs.setTheme(listOf("DARK", "LIGHT", "SYSTEM")[it]) }

            SectionLabel("Money")
            VCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                NavRow("Default currency", "$cur · ${Money.name(cur)}") { pickCur = true }
                HorizontalDivider(color = c.line)
                NavRow("Budgets", "Monthly limits with alerts") { nav.navigate(Routes.BUDGETS) }
                HorizontalDivider(color = c.line)
                NavRow("Bank alerts", "Turn bank SMS into entries automatically") { nav.navigate(Routes.BANK) }
                HorizontalDivider(color = c.line)
                NavRow("Recurring & subscriptions", "Salary, rent, Netflix, commission reminders…") { nav.navigate(Routes.SUBSCRIPTIONS) }
                HorizontalDivider(color = c.line)
                NavRow("Categories", "Add, rename or hide categories") { nav.navigate(Routes.CATEGORIES) }
                HorizontalDivider(color = c.line)
                NavRow("Businesses", "Manage the books in Business mode") { nav.navigate(Routes.BUSINESSES) }
            }

            SectionLabel("Security & data")
            VCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                NavRow("Security", "PIN, fingerprint, auto-lock, screenshots") { nav.navigate(Routes.SECURITY) }
                HorizontalDivider(color = c.line)
                NavRow(
                    "Backup & restore",
                    if (lastBackup == 0L) "Never backed up" else "Last backup ${Dates.fmt(lastBackup)}"
                ) { nav.navigate(Routes.BACKUP) }
                HorizontalDivider(color = c.line)
                NavRow("Lock now", "Hide everything behind your PIN") { activity.lockNow() }
                HorizontalDivider(color = c.line)
                NavRow(
                    "App updates",
                    "Version ${com.kingsman.vault.BuildConfig.VERSION_NAME}" +
                        if (vm.prefs.availableUpdate.collectAsState().value.isNotBlank()) " · update available" else ""
                ) { nav.navigate(Routes.UPDATES) }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Vault ${com.kingsman.vault.BuildConfig.VERSION_NAME} · Track • Plan • Grow\nEverything stays on this phone, encrypted.",
                color = c.muted, fontSize = 12.sp
            )
        }
    }
    if (pickCur) CurrencyPickerDialog(cur, { prefs.setDefaultCurrency(it); pickCur = false }) { pickCur = false }
}

// ============================== Businesses ==============================

@Composable
fun BusinessesScreen(vm: VaultViewModel, onBack: () -> Unit) {
    val c = LocalVault.current
    val list by vm.businesses.collectAsState()
    var adding by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<Business?>(null) }
    var renaming by remember { mutableStateOf<Business?>(null) }
    var archiving by remember { mutableStateOf<Business?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Businesses", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryButton("+ Add business", Modifier.fillMaxWidth()) { adding = true }
            if (list.isEmpty()) EmptyNote("No businesses yet. Each business gets its own separate books in Business mode.")
            list.forEach { b ->
                VCard(Modifier.fillMaxWidth(), onClick = { open = b }) {
                    Text(b.name, color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
    if (adding) TextInputDialog("New business", label = "Business name", confirm = "Add", onDismiss = { adding = false }) {
        vm.addBusiness(it); adding = false
    }
    open?.let { b ->
        AlertDialog(
            onDismissRequest = { open = null },
            title = { Text(b.name, fontWeight = FontWeight.ExtraBold) },
            text = { Text("Hiding a business keeps its records but removes it from Business mode.") },
            confirmButton = { TextButton(onClick = { renaming = b; open = null }) { Text("Rename") } },
            dismissButton = { TextButton(onClick = { archiving = b; open = null }) { Text("Hide", color = c.exp) } }
        )
    }
    renaming?.let { b ->
        TextInputDialog("Rename business", initial = b.name, onDismiss = { renaming = null }) {
            vm.renameBusiness(b, it); renaming = null
        }
    }
    archiving?.let { b ->
        ConfirmDialog("Hide ${b.name}?", "Its records stay in the database and in backups.", confirm = "Hide", onDismiss = { archiving = null }) {
            vm.archiveBusiness(b)
        }
    }
}

// ============================== Categories ==============================

@Composable
fun CategoriesScreen(vm: VaultViewModel, onBack: () -> Unit) {
    val c = LocalVault.current
    val all by vm.categories.collectAsState()
    var scopeIdx by remember { mutableStateOf(0) }
    var kindIdx by remember { mutableStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf<Category?>(null) }
    var renaming by remember { mutableStateOf<Category?>(null) }

    val scope = if (scopeIdx == 0) Scope.PERSONAL else Scope.BUSINESS
    val kind = if (kindIdx == 0) Kind.EXPENSE else Kind.INCOME
    val list = all.filter { it.scope == scope && it.kind == kind }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Categories", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Segmented(listOf("Personal", "Business"), scopeIdx) { scopeIdx = it }
            Segmented(listOf("Expense", "Income"), kindIdx) { kindIdx = it }
            PrimaryButton("+ Add category", Modifier.fillMaxWidth()) { adding = true }
            VCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                list.forEachIndexed { i, cat ->
                    if (i > 0) HorizontalDivider(color = c.line)
                    NavRow(cat.name) { open = cat }
                }
                if (list.isEmpty()) Text("No categories here yet.", color = c.muted)
            }
        }
    }
    if (adding) TextInputDialog("New category", label = "Name", confirm = "Add", onDismiss = { adding = false }) {
        vm.addCategory(it, kind, scope); adding = false
    }
    open?.let { cat ->
        AlertDialog(
            onDismissRequest = { open = null },
            title = { Text(cat.name, fontWeight = FontWeight.ExtraBold) },
            text = { Text("Hidden categories keep their past entries; they just won't show when adding new ones.") },
            confirmButton = { TextButton(onClick = { renaming = cat; open = null }) { Text("Rename") } },
            dismissButton = { TextButton(onClick = { vm.archiveCategory(cat); open = null }) { Text("Hide", color = c.exp) } }
        )
    }
    renaming?.let { cat ->
        TextInputDialog("Rename category", initial = cat.name, onDismiss = { renaming = null }) {
            vm.renameCategory(cat, it); renaming = null
        }
    }
}

// ============================== Recurring & subscriptions ==============================

@Composable
fun SubscriptionsScreen(vm: VaultViewModel, onBack: () -> Unit) {
    val c = LocalVault.current
    val items by vm.scopedRecurring.collectAsState()
    val scope by vm.scope.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<Recurring?>(null) }
    var adding by remember { mutableStateOf(false) }

    val shown = items.filter { it.isSubscription == (tab == 0) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Recurring", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Segmented(listOf("Subscriptions", "Other recurring"), tab) { tab = it }
            if (scope == NO_BIZ) {
                EmptyNote("Add a business on the Home screen first.")
            } else {
                if (tab == 0) {
                    val monthly = Calc.sumByCurrency(
                        shown.filter { it.active && !it.variable }.map { it.currency to Calc.monthlyEquivalent(it) }
                    )
                    if (monthly.isNotEmpty()) {
                        VCard(Modifier.fillMaxWidth()) {
                            Text("Subscriptions cost you", color = c.muted, fontSize = 13.sp)
                            monthly.forEach { (code, amt) -> AmountText(amt, code, size = 22, prefix = "", modifier = Modifier) }
                            Text("per month", color = c.muted, fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(
                        "Fixed amounts are logged automatically on their date. Amounts that change (like commission) show up on Home for you to enter.",
                        color = c.muted, fontSize = 13.sp
                    )
                }
                PrimaryButton(if (tab == 0) "+ Add subscription" else "+ Add recurring entry", Modifier.fillMaxWidth()) { adding = true }
                if (shown.isEmpty()) EmptyNote(if (tab == 0) "Netflix, DSTV, Spotify, gym, iCloud… add them to see what they really cost." else "Salary, rent, staff wages, loan repayments, commission…")
                shown.forEach { r ->
                    VCard(Modifier.fillMaxWidth(), onClick = { editing = r }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.title, color = if (r.active) c.text else c.muted, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Text(
                                    "${Frequency.label(r.frequency)} · " +
                                        (if (r.active) "next ${Dates.fmt(r.nextDate)}" else "paused") +
                                        " · ${r.categoryName}",
                                    color = c.muted, fontSize = 12.sp
                                )
                            }
                            if (r.variable) Text("Varies", color = c.accText, fontWeight = FontWeight.Bold)
                            else AmountText(r.amount, r.currency, size = 15, color = if (r.kind == Kind.INCOME) c.inc else c.text)
                        }
                    }
                }
            }
        }
    }
    if (adding) RecurringDialog(vm, null, tab == 0, scope.takeIf { it != NO_BIZ }) { adding = false }
    editing?.let { r -> RecurringDialog(vm, r, r.isSubscription, r.businessId) { editing = null } }
}

@Composable
private fun RecurringDialog(
    vm: VaultViewModel,
    existing: Recurring?,
    subscription: Boolean,
    businessId: Long?,
    onClose: () -> Unit
) {
    val c = LocalVault.current
    val ctx = LocalContext.current
    val categories by vm.categories.collectAsState()

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var kindIdx by remember { mutableStateOf(if (existing?.kind == Kind.INCOME) 1 else 0) }
    var variable by remember { mutableStateOf(existing?.variable ?: false) }
    var amount by remember { mutableStateOf(existing?.takeIf { it.amount > 0 }?.let { Money.toInput(it.amount) } ?: "") }
    var currency by remember { mutableStateOf(existing?.currency ?: vm.prefs.defaultCurrency.value) }
    var catId by remember { mutableStateOf(existing?.categoryId) }
    var freq by remember { mutableStateOf(existing?.frequency ?: Frequency.MONTHLY) }
    var next by remember { mutableLongStateOf(existing?.nextDate ?: Dates.startOfDay(Dates.today())) }
    var isSub by remember { mutableStateOf(existing?.isSubscription ?: subscription) }
    var active by remember { mutableStateOf(existing?.active ?: true) }
    var pickCur by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var catMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val kind = if (kindIdx == 1) Kind.INCOME else Kind.EXPENSE
    val scope = if (businessId == null) Scope.PERSONAL else Scope.BUSINESS
    val cats = categories.filter { it.kind == kind && it.scope == scope }
    val cat = categories.firstOrNull { it.id == catId }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (existing == null) (if (subscription) "New subscription" else "New recurring entry") else "Edit", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Field(title, { title = it }, if (isSub) "Name, e.g. Netflix" else "Name, e.g. Salary")
                Segmented(listOf("Expense", "Income"), kindIdx) { if (it != kindIdx) { kindIdx = it; catId = null } }
                SettingSwitch("Amount changes each time", "e.g. commission — you'll be asked to enter it", variable) { variable = it }
                if (!variable) Field(amount, { amount = it }, "Amount", number = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VChip("$currency ▾") { pickCur = true }
                    Box {
                        VChip((cat?.name ?: "Category") + " ▾", selected = cat != null) { catMenu = true }
                        DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                            cats.forEach { k ->
                                DropdownMenuItem(text = { Text(k.name) }, onClick = { catId = k.id; catMenu = false })
                            }
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Frequency.all.forEach { f -> VChip(Frequency.label(f), f == freq) { freq = f } }
                }
                VChip((if (existing == null) "Starts " else "Next ") + Dates.fmt(next)) { pickDate = true }
                SettingSwitch("Subscription", "Show in the subscriptions total", isSub) { isSub = it }
                if (existing != null) {
                    SettingSwitch("Active", "Pause without deleting", active) { active = it }
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = c.exp) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = if (variable) 0L else (Money.parse(amount) ?: 0L)
                val chosen = cat
                when {
                    title.isBlank() -> ctx.toast("Enter a name")
                    chosen == null -> ctx.toast("Pick a category")
                    !variable && a <= 0 -> ctx.toast("Enter an amount")
                    else -> {
                        vm.saveRecurring(
                            Recurring(
                                id = existing?.id ?: 0L,
                                title = title.trim(), kind = kind, amount = a, variable = variable,
                                currency = currency, categoryId = chosen.id, categoryName = chosen.name,
                                businessId = businessId, frequency = freq, nextDate = next,
                                isSubscription = isSub, active = active, note = existing?.note ?: ""
                            )
                        )
                        onClose()
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }
    )
    if (pickCur) CurrencyPickerDialog(currency, { currency = it; pickCur = false }) { pickCur = false }
    if (pickDate) DatePickDialog(next, { next = Dates.startOfDay(it) }) { pickDate = false }
    if (confirmDelete && existing != null) {
        ConfirmDialog("Delete \"${existing.title}\"?", "Entries already logged stay in your records.", onDismiss = { confirmDelete = false }) {
            vm.deleteRecurring(existing); onClose()
        }
    }
}

// ============================== Security ==============================

@Composable
fun SecurityScreen(vm: VaultViewModel, activity: MainActivity, onBack: () -> Unit) {
    val c = LocalVault.current
    val ctx = LocalContext.current
    val app = activity.application as VaultApp
    val prefs = vm.prefs
    val bio by prefs.biometric.collectAsState()
    val autoLock by prefs.autoLockSec.collectAsState()
    val shots by prefs.blockScreenshots.collectAsState()
    val wipe by prefs.wipeOnFail.collectAsState()
    val bioAvailable = remember { activity.canUseBiometric() }
    var changePin by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Security", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                NavRow("Change PIN", "${prefs.pinLength}-digit PIN") { changePin = true }
                HorizontalDivider(color = c.line)
                SettingSwitch(
                    "Unlock with fingerprint",
                    if (bioAvailable) "Use your fingerprint instead of typing the PIN" else "No fingerprint is set up on this phone",
                    bio && bioAvailable,
                    enabled = bioAvailable
                ) { on ->
                    if (on) activity.promptBiometric { prefs.setBiometric(true) } else prefs.setBiometric(false)
                }
            }

            SectionLabel("Auto-lock when you leave the app")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Immediately", 30 to "30 sec", 60 to "1 min", 300 to "5 min").forEach { (s, label) ->
                    VChip(label, autoLock == s) { prefs.setAutoLockSec(s) }
                }
            }

            VCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                SettingSwitch(
                    "Block screenshots",
                    "Also hides Vault in the recent-apps screen and from screen recording",
                    shots
                ) { prefs.setBlockScreenshots(it) }
                HorizontalDivider(color = c.line)
                SettingSwitch(
                    "Erase data after 10 wrong PINs",
                    "Protects you if the phone is stolen. Keep a backup!",
                    wipe
                ) { if (it) confirmWipe = true else prefs.setWipeOnFail(false) }
            }

            VCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                Text("How Vault protects your data", color = c.text, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "• Your records are stored in an encrypted database (SQLCipher, 256-bit AES).\n" +
                        "• The database key is locked inside Android's hardware Keystore.\n" +
                        "• Your PIN is never stored — only a salted hash.\n" +
                        "• Backups are encrypted with a password only you know.\n" +
                        "• Android cloud backup is switched off, so nothing leaves the phone without you.",
                    color = c.soft, fontSize = 13.sp
                )
            }
            SecondaryButton("Lock now", Modifier.fillMaxWidth()) { activity.lockNow() }
        }
    }

    if (changePin) ChangePinDialog(app, onDone = { msg -> ctx.toast(msg) }) { changePin = false }
    if (confirmWipe) {
        ConfirmDialog(
            "Turn on auto-erase?",
            "After 10 wrong PIN attempts in a row, every record in Vault will be permanently erased. Make sure you have a recent backup.",
            confirm = "Turn on",
            onDismiss = { confirmWipe = false }
        ) { prefs.setWipeOnFail(true) }
    }
}

@Composable
private fun ChangePinDialog(app: VaultApp, onDone: (String) -> Unit, onClose: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var new1 by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val c = LocalVault.current

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Change PIN", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(current, { current = it.filter(Char::isDigit).take(6) }, "Current PIN", password = true)
                Field(new1, { new1 = it.filter(Char::isDigit).take(6) }, "New PIN (4–6 digits)", password = true)
                Field(new2, { new2 = it.filter(Char::isDigit).take(6) }, "Repeat new PIN", password = true)
                if (error.isNotEmpty()) Text(error, color = c.exp, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                when {
                    new1.length < 4 -> error = "PIN must be 4–6 digits"
                    new1 != new2 -> error = "New PINs don't match"
                    else -> {
                        busy = true
                        scope.launch {
                            val ok = withContext(Dispatchers.Default) { app.security.verifyPin(current) }
                            if (!ok) {
                                error = "Current PIN is wrong"
                            } else {
                                withContext(Dispatchers.Default) { app.security.setPin(new1) }
                                onDone("PIN changed")
                                onClose()
                            }
                            busy = false
                        }
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }
    )
}

// ============================== Backup ==============================

@Composable
fun BackupScreen(vm: VaultViewModel, activity: MainActivity, onBack: () -> Unit) {
    val c = LocalVault.current
    val ctx = LocalContext.current
    val lastBackup by vm.prefs.lastBackup.collectAsState()
    var pw1 by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    var withReceipts by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restorePw by remember { mutableStateOf("") }
    var confirmRestore by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) restoreUri = uri
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Backup & restore", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VCard(Modifier.fillMaxWidth()) {
                Text("Back up by email", color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Vault creates one encrypted file with all your records. Send it to your own email — " +
                        "nobody can open it without the password you set here, not even the email provider.",
                    color = c.soft, fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (lastBackup == 0L) "No backup yet" else "Last backup: ${Dates.fmt(lastBackup)}",
                    color = c.accText, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Field(pw1, { pw1 = it }, "Backup password (min 6 characters)", password = true)
                Spacer(Modifier.height(8.dp))
                Field(pw2, { pw2 = it }, "Repeat password", password = true)
                Spacer(Modifier.height(6.dp))
                Text("Write this password down. Without it, the backup can't be restored.", color = c.muted, fontSize = 12.sp)
                SettingSwitch(
                    "Include receipt photos",
                    "Makes the file bigger — turn off if your email rejects it",
                    withReceipts
                ) { withReceipts = it }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(if (busy) "Encrypting…" else "Create backup & email it", Modifier.fillMaxWidth(), enabled = !busy) {
                    when {
                        pw1.length < 6 -> ctx.toast("Use at least 6 characters")
                        pw1 != pw2 -> ctx.toast("Passwords don't match")
                        else -> {
                            busy = true
                            vm.exportBackup(pw1, withReceipts) { file, err ->
                                busy = false
                                if (file != null) {
                                    pw1 = ""; pw2 = ""
                                    activity.shareFile(
                                        file, "application/octet-stream",
                                        "Vault backup ${Dates.fmt(System.currentTimeMillis())}",
                                        "Encrypted Vault backup. To restore: open Vault > More > Backup & restore > Restore, pick this file and enter your backup password."
                                    )
                                } else ctx.toast("Backup failed: ${err ?: "unknown error"}")
                            }
                        }
                    }
                }
            }

            VCard(Modifier.fillMaxWidth()) {
                Text("Restore", color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Download the .vault file from your email, then pick it here. Restoring replaces everything currently in Vault.",
                    color = c.soft, fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Choose backup file", Modifier.fillMaxWidth()) {
                    activity.prepareExternal()
                    picker.launch(arrayOf("*/*"))
                }
            }
        }
    }

    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null; restorePw = "" },
            title = { Text("Backup password", fontWeight = FontWeight.ExtraBold) },
            text = { Field(restorePw, { restorePw = it }, "Password", password = true) },
            confirmButton = {
                TextButton(enabled = restorePw.isNotEmpty(), onClick = { confirmRestore = true }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null; restorePw = "" }) { Text("Cancel") } }
        )
        if (confirmRestore) {
            ConfirmDialog(
                "Replace all data?",
                "Everything currently in Vault will be replaced with the backup's contents.",
                confirm = "Replace",
                onDismiss = { confirmRestore = false }
            ) {
                val pw = restorePw
                restoreUri = null
                restorePw = ""
                vm.importBackup(uri, pw) { err ->
                    ctx.toast(err ?: "Backup restored")
                }
            }
        }
    }
}
