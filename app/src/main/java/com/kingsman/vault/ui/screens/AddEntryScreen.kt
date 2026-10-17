package com.kingsman.vault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.core.content.FileProvider
import com.kingsman.vault.MainActivity
import com.kingsman.vault.data.Category
import com.kingsman.vault.data.Kind
import com.kingsman.vault.data.Recurring
import com.kingsman.vault.data.Scope
import com.kingsman.vault.data.Txn
import com.kingsman.vault.ui.ConfirmDialog
import com.kingsman.vault.ui.CurrencyPickerDialog
import com.kingsman.vault.ui.DatePickDialog
import com.kingsman.vault.ui.IconSquare
import com.kingsman.vault.ui.Keypad
import com.kingsman.vault.ui.NO_BIZ
import com.kingsman.vault.ui.PrimaryButton
import com.kingsman.vault.ui.SecondaryButton
import com.kingsman.vault.ui.Segmented
import com.kingsman.vault.ui.TextInputDialog
import com.kingsman.vault.ui.VChip
import com.kingsman.vault.ui.VaultViewModel
import com.kingsman.vault.ui.applyKey
import com.kingsman.vault.ui.backspace
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.ui.theme.Num
import com.kingsman.vault.ui.toast
import com.kingsman.vault.util.Dates
import com.kingsman.vault.util.Money
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/** Tracks receipt photos taken on this screen so unsaved ones are deleted when you leave. */
private class ReceiptSession {
    var saved = false
    val attached = mutableListOf<String>()
}

/** "48500.5" -> "48,500.5" while typing. */
fun displayAmount(raw: String): String {
    val parts = raw.split(".")
    val int = parts[0].toLongOrNull() ?: 0L
    val grouped = String.format(Locale.US, "%,d", int)
    return if (parts.size > 1) "$grouped.${parts[1]}" else grouped
}

@Composable
fun AddEntryScreen(
    vm: VaultViewModel,
    txnId: Long,
    recurringId: Long,
    suggestionId: Long,
    activity: MainActivity,
    onDone: () -> Unit
) {
    val c = LocalVault.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = vm.prefs
    val businesses by vm.businesses.collectAsState()
    val categories by vm.categories.collectAsState()
    val usage by vm.usage.collectAsState()

    var editing by remember { mutableStateOf<Txn?>(null) }
    var rec by remember { mutableStateOf<Recurring?>(null) }
    var kind by remember { mutableStateOf(Kind.EXPENSE) }
    var amount by remember { mutableStateOf("0") }
    var currency by remember { mutableStateOf(prefs.defaultCurrency.value) }
    var catId by remember { mutableStateOf<Long?>(null) }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var paidPersonally by remember { mutableStateOf(false) }
    var bizId by remember { mutableStateOf(vm.scope.value.takeIf { it != NO_BIZ }) }

    var showCurrency by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var showTag by remember { mutableStateOf(false) }
    var showNewCat by remember { mutableStateOf(false) }
    var showBook by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // ----- Receipt photo -----
    var receipt by remember { mutableStateOf<String?>(null) }
    var showReceiptMenu by remember { mutableStateOf(false) }
    var viewReceipt by remember { mutableStateOf(false) }
    var captureTarget by remember { mutableStateOf<File?>(null) }
    val session = remember { ReceiptSession() }

    fun setReceipt(name: String) {
        receipt?.let { old -> if (old in session.attached) { vm.discardReceipt(old); session.attached.remove(old) } }
        receipt = name
        session.attached += name
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = captureTarget
        if (ok && f != null) scope.launch {
            vm.importReceipt(f)?.let { setReceipt(it) } ?: ctx.toast("Couldn't read the photo")
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            vm.importReceipt(uri)?.let { setReceipt(it) } ?: ctx.toast("Couldn't read that image")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!session.saved) session.attached.forEach { vm.discardReceipt(it) }
        }
    }

    LaunchedEffect(txnId, recurringId) {
        if (txnId > 0) vm.getTxn(txnId)?.let { t ->
            editing = t; kind = t.kind; amount = Money.toInput(t.amount); currency = t.currency
            catId = t.categoryId; date = t.date; note = t.note; tag = t.tag
            paidPersonally = t.paidPersonally; bizId = t.businessId; receipt = t.receiptPath
        }
        if (suggestionId > 0) vm.getSuggestion(suggestionId)?.let { sg ->
            kind = sg.kind; amount = Money.toInput(sg.amount); currency = sg.currency
            note = sg.description; date = sg.date
        }
        if (recurringId > 0) vm.getRecurring(recurringId)?.let { r ->
            rec = r; kind = r.kind; currency = r.currency; catId = r.categoryId
            note = r.title; bizId = r.businessId; date = r.nextDate
            if (!r.variable && r.amount > 0) amount = Money.toInput(r.amount)
        }
    }

    val catScope = if (bizId == null) Scope.PERSONAL else Scope.BUSINESS
    val visibleCats = categories
        .filter { it.kind == kind && it.scope == catScope }
        .sortedWith(compareByDescending<Category> { usage[it.id] ?: 0 }.thenBy { it.name.lowercase() })
    val bookName = bizId?.let { id -> businesses.firstOrNull { it.id == id }?.name } ?: "Personal"
    val kindLabel = if (kind == Kind.INCOME) "income" else "expense"

    fun save(andNew: Boolean) {
        val minor = Money.parse(amount) ?: 0L
        if (minor <= 0) { ctx.toast("Enter an amount"); return }
        val cat = categories.firstOrNull { it.id == catId }
        if (cat == null) { ctx.toast("Pick a category"); return }
        val e = editing
        val txn = Txn(
            id = e?.id ?: 0L,
            kind = kind,
            amount = minor,
            currency = currency,
            categoryId = cat.id,
            categoryName = cat.name,
            businessId = bizId,
            date = date,
            note = note,
            tag = tag,
            paidPersonally = bizId != null && kind == Kind.EXPENSE && paidPersonally,
            reimbursed = e?.reimbursed ?: false,
            recurringId = e?.recurringId,
            createdAt = e?.createdAt ?: System.currentTimeMillis(),
            receiptPath = receipt
        )
        session.saved = true
        session.attached.clear()
        if (suggestionId > 0) vm.markSuggestionAdded(suggestionId)
        val r = rec
        if (r != null) {
            vm.confirmRecurring(r, txn)
            onDone()
            return
        }
        vm.saveTxn(txn, previousReceipt = e?.receiptPath)
        if (andNew) {
            amount = "0"; note = ""; tag = ""; receipt = null
            session.saved = false
            ctx.toast("Saved")
        } else {
            onDone()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconSquare(Icons.Rounded.Close, "Close", onClick = onDone)
                Spacer(Modifier.width(10.dp))
                Segmented(
                    listOf("Expense", "Income"),
                    if (kind == Kind.INCOME) 1 else 0,
                    Modifier.weight(1f)
                ) {
                    val k = if (it == 1) Kind.INCOME else Kind.EXPENSE
                    if (k != kind) { kind = k; catId = null }
                }
            }

            rec?.let {
                Text(
                    "Confirm: ${it.title}", color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp
                )
            }
            if (editing != null) {
                Text("Edit entry", color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            if (suggestionId > 0) {
                Text("From a bank alert — check the details, pick a category, save.", color = c.muted, fontSize = 13.sp)
            }

            // Which book: Personal or a business
            Box {
                VChip(
                    (if (bizId == null) "PERSONAL" else "BUSINESS · ${bookName.uppercase()}") + "  ▾",
                    selected = true
                ) { showBook = true }
                DropdownMenu(expanded = showBook, onDismissRequest = { showBook = false }) {
                    DropdownMenuItem(text = { Text("Personal") }, onClick = {
                        if (bizId != null) catId = null
                        bizId = null; showBook = false
                    })
                    businesses.forEach { b ->
                        DropdownMenuItem(text = { Text(b.name) }, onClick = {
                            if (bizId == null) catId = null
                            bizId = b.id; showBook = false
                        })
                    }
                }
            }

            // Amount
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    displayAmount(amount),
                    color = c.text,
                    style = Num.copy(fontSize = 44.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                VChip("${Money.symbol(currency).trim()} $currency  ▾") { showCurrency = true }
            }
            HorizontalDivider(color = c.line)

            // Categories
            val cells = visibleCats.map { it.id to it.name } + (-1L to "+ New")
            cells.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (id, name) ->
                        VChip(
                            name,
                            selected = id == catId,
                            dashed = id == -1L,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            if (id == -1L) showNewCat = true else catId = id
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            // Details
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VChip(
                    if (Dates.toLocalDate(date) == Dates.today()) "Today" else Dates.fmt(date)
                ) { showDate = true }
                VChip(if (note.isBlank()) "Note" else note, selected = note.isNotBlank(), modifier = Modifier.weight(1f, fill = false)) { showNote = true }
                VChip(if (tag.isBlank()) "Tag" else "#$tag", selected = tag.isNotBlank()) { showTag = true }
            }
            Box {
                VChip(
                    if (receipt != null) "Receipt photo ✓" else "Add receipt photo",
                    selected = receipt != null
                ) { showReceiptMenu = true }
                DropdownMenu(expanded = showReceiptMenu, onDismissRequest = { showReceiptMenu = false }) {
                    DropdownMenuItem(text = { Text("Take photo") }, onClick = {
                        showReceiptMenu = false
                        runCatching {
                            val f = vm.captureFile()
                            captureTarget = f
                            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
                            activity.prepareExternal()
                            takePicture.launch(uri)
                        }.onFailure { ctx.toast("No camera app found") }
                    })
                    DropdownMenuItem(text = { Text("Choose from gallery") }, onClick = {
                        showReceiptMenu = false
                        activity.prepareExternal()
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
                    if (receipt != null) {
                        DropdownMenuItem(text = { Text("View") }, onClick = { showReceiptMenu = false; viewReceipt = true })
                        DropdownMenuItem(text = { Text("Remove") }, onClick = {
                            showReceiptMenu = false
                            receipt?.let { old -> if (old in session.attached) { vm.discardReceipt(old); session.attached.remove(old) } }
                            receipt = null
                        })
                    }
                }
            }
            if (bizId != null && kind == Kind.EXPENSE) {
                VChip(
                    if (paidPersonally) "Paid from my personal money ✓" else "Paid from my personal money?",
                    selected = paidPersonally
                ) { paidPersonally = !paidPersonally }
            }
            Spacer(Modifier.height(4.dp))
        }

        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Keypad(
                onKey = { amount = applyKey(amount, it) },
                onBackspace = { amount = backspace(amount) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val r = rec
                when {
                    r != null -> SecondaryButton("Skip", Modifier.weight(1f)) { vm.skipRecurring(r); onDone() }
                    editing != null -> SecondaryButton("Delete", Modifier.weight(1f), color = c.exp) { confirmDelete = true }
                    else -> SecondaryButton("Save + new", Modifier.weight(1f)) { save(andNew = true) }
                }
                PrimaryButton(
                    if (editing != null) "Update" else "Save $kindLabel",
                    Modifier.weight(2f)
                ) { save(andNew = false) }
            }
        }
    }

    if (viewReceipt) {
        receipt?.let { ReceiptViewerDialog(vm, it) { viewReceipt = false } }
    }
    if (showCurrency) {
        CurrencyPickerDialog(currency, onPick = {
            currency = it
            prefs.setDefaultCurrency(it) // the chosen currency stays the default until changed
            showCurrency = false
        }, onDismiss = { showCurrency = false })
    }
    if (showDate) {
        DatePickDialog(date, onPick = { date = Dates.withNowTime(it) }, onDismiss = { showDate = false })
    }
    if (showNote) {
        TextInputDialog("Note", initial = note, label = "What was it for?", allowEmpty = true, onDismiss = { showNote = false }) {
            note = it; showNote = false
        }
    }
    if (showTag) {
        TextInputDialog("Tag", initial = tag, label = "e.g. project, client, trip", allowEmpty = true, onDismiss = { showTag = false }) {
            tag = it.removePrefix("#"); showTag = false
        }
    }
    if (showNewCat) {
        TextInputDialog("New ${kindLabel} category", label = "Name", confirm = "Add", onDismiss = { showNewCat = false }) { name ->
            vm.addCategory(name, kind, catScope) { created -> catId = created.id }
            showNewCat = false
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            "Delete entry?", "This can't be undone.",
            onDismiss = { confirmDelete = false }
        ) {
            editing?.let { vm.deleteTxn(it) }
            onDone()
        }
    }
}
