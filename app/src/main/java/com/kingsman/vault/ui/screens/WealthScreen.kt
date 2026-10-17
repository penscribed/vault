package com.kingsman.vault.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingsman.vault.data.Debt
import com.kingsman.vault.data.Direction
import com.kingsman.vault.data.Goal
import com.kingsman.vault.data.Investment
import com.kingsman.vault.ui.AmountText
import com.kingsman.vault.ui.ConfirmDialog
import com.kingsman.vault.ui.CurrencyPickerDialog
import com.kingsman.vault.ui.DatePickDialog
import com.kingsman.vault.ui.EmptyNote
import com.kingsman.vault.ui.Field
import com.kingsman.vault.ui.NO_BIZ
import com.kingsman.vault.ui.PrimaryButton
import com.kingsman.vault.ui.ProgressLine
import com.kingsman.vault.ui.ScreenHeader
import com.kingsman.vault.ui.SectionLabel
import com.kingsman.vault.ui.Segmented
import com.kingsman.vault.ui.VCard
import com.kingsman.vault.ui.VChip
import com.kingsman.vault.ui.VaultViewModel
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.ui.toast
import com.kingsman.vault.util.Calc
import com.kingsman.vault.util.Dates
import com.kingsman.vault.util.Money

private val investmentTypes = listOf(
    "Stocks", "Mutual fund", "T-bills & bonds", "Fixed deposit", "Real estate",
    "Business stake", "Crypto", "Retirement", "Other"
)

@Composable
fun WealthScreen(vm: VaultViewModel) {
    var tab by remember { mutableStateOf(0) }
    var adding by remember { mutableStateOf(false) }
    val scope by vm.scope.collectAsState()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Wealth")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Segmented(listOf("Savings", "Investments", "Debts"), tab) { tab = it }
            if (scope == NO_BIZ) {
                EmptyNote("Add a business on the Home screen first.")
            } else {
                PrimaryButton(
                    when (tab) { 0 -> "+ New savings goal"; 1 -> "+ New investment"; else -> "+ New debt" },
                    Modifier.fillMaxWidth()
                ) { adding = true }
                when (tab) {
                    0 -> SavingsTab(vm)
                    1 -> InvestmentsTab(vm)
                    else -> DebtsTab(vm)
                }
            }
        }
    }

    if (adding) {
        val bid = scope
        when (tab) {
            0 -> GoalEditDialog(vm, null, bid) { adding = false }
            1 -> InvestmentAddDialog(vm, bid) { adding = false }
            else -> DebtAddDialog(vm, bid) { adding = false }
        }
    }
}

// ======================= Savings =======================

@Composable
private fun SavingsTab(vm: VaultViewModel) {
    val c = LocalVault.current
    val goals by vm.scopedGoals.collectAsState()
    val entries by vm.goalEntries.collectAsState()
    var open by remember { mutableStateOf<Goal?>(null) }

    if (goals.isEmpty()) EmptyNote("Name a goal — an emergency fund, a car, school fees — and track every naira or dollar you put towards it.")

    val byCur = Calc.sumByCurrency(goals.map { it.currency to Calc.saved(it, entries) })
    if (byCur.isNotEmpty()) {
        SectionLabel("Total saved")
        byCur.forEach { (code, amt) -> AmountText(amt, code, size = 24) }
    }

    goals.forEach { g ->
        val saved = Calc.saved(g, entries)
        val frac = if (g.target > 0) saved.toFloat() / g.target else 0f
        VCard(Modifier.fillMaxWidth(), onClick = { open = g }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(g.name, color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("${(frac * 100).toInt()}%", color = c.accText, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(6.dp))
            Row {
                AmountText(saved, g.currency, size = 14)
                AmountText(g.target, g.currency, size = 14, prefix = "  of ", color = c.muted, weight = FontWeight.Normal)
            }
            Spacer(Modifier.height(8.dp))
            ProgressLine(frac)
            g.deadline?.let {
                Spacer(Modifier.height(6.dp))
                Text("Target date ${Dates.fmt(it)}", color = c.muted, fontSize = 12.sp)
            }
        }
    }

    val openGoal = open?.let { g -> goals.firstOrNull { it.id == g.id } }
    if (openGoal != null) GoalDetailDialog(vm, openGoal) { open = null }
}

@Composable
private fun GoalEditDialog(vm: VaultViewModel, goal: Goal?, businessId: Long?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf(goal?.name ?: "") }
    var target by remember { mutableStateOf(goal?.let { Money.toInput(it.target) } ?: "") }
    var currency by remember { mutableStateOf(goal?.currency ?: vm.prefs.defaultCurrency.value) }
    var deadline by remember { mutableStateOf(goal?.deadline) }
    var pickCur by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (goal == null) "New savings goal" else "Edit goal", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(name, { name = it }, "Goal name (anything you like)")
                Field(target, { target = it }, "Target amount", number = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VChip("$currency ▾") { pickCur = true }
                    VChip(deadline?.let { "By ${Dates.fmt(it)}" } ?: "Target date (optional)") { pickDate = true }
                }
                if (deadline != null) TextButton(onClick = { deadline = null }) { Text("Remove target date") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = Money.parse(target)
                when {
                    name.isBlank() -> ctx.toast("Give the goal a name")
                    t == null || t <= 0 -> ctx.toast("Enter a target amount")
                    else -> {
                        vm.saveGoal(
                            (goal ?: Goal(name = name, target = t, currency = currency, businessId = businessId))
                                .copy(name = name.trim(), target = t, currency = currency, deadline = deadline)
                        )
                        onClose()
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }
    )
    if (pickCur) CurrencyPickerDialog(currency, { currency = it; pickCur = false }) { pickCur = false }
    if (pickDate) DatePickDialog(deadline ?: System.currentTimeMillis(), { deadline = Dates.startOfDay(it) }) { pickDate = false }
}

@Composable
private fun GoalDetailDialog(vm: VaultViewModel, g: Goal, onClose: () -> Unit) {
    val c = LocalVault.current
    val ctx = LocalContext.current
    val entries by vm.goalEntries.collectAsState()
    val mine = entries.filter { it.goalId == g.id }
    val saved = mine.sumOf { it.amount }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun move(sign: Int) {
        val a = Money.parse(amount)
        if (a == null || a <= 0) { ctx.toast("Enter an amount"); return }
        vm.goalMove(g, a * sign, note.trim())
        amount = ""; note = ""
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(g.name, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row {
                    AmountText(saved, g.currency, size = 20)
                    AmountText(g.target, g.currency, size = 14, prefix = "  of ", color = c.muted, weight = FontWeight.Normal)
                }
                ProgressLine(if (g.target > 0) saved.toFloat() / g.target else 0f)
                val left = g.target - saved
                if (left > 0) AmountText(left, g.currency, size = 13, prefix = "Still to save: ", color = c.muted, weight = FontWeight.Normal)
                Field(amount, { amount = it }, "Amount", number = true)
                Field(note, { note = it }, "Note (optional)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VChip("Add money", selected = true, modifier = Modifier.weight(1f)) { move(1) }
                    VChip("Withdraw", modifier = Modifier.weight(1f)) { move(-1) }
                }
                if (mine.isNotEmpty()) {
                    SectionLabel("History")
                    mine.take(6).forEach { e ->
                        Row {
                            Text(Dates.fmt(e.date) + if (e.note.isNotBlank()) " · ${e.note}" else "", color = c.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            AmountText(e.amount, g.currency, size = 12, signed = true)
                        }
                    }
                }
                Row {
                    TextButton(onClick = { edit = true }) { Text("Edit goal") }
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = c.exp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } }
    )
    if (edit) GoalEditDialog(vm, g, g.businessId) { edit = false }
    if (confirmDelete) ConfirmDialog("Delete \"${g.name}\"?", "The goal and its history will be removed.", onDismiss = { confirmDelete = false }) {
        vm.deleteGoal(g); onClose()
    }
}

// ======================= Investments =======================

@Composable
private fun InvestmentsTab(vm: VaultViewModel) {
    val c = LocalVault.current
    val invs by vm.scopedInvestments.collectAsState()
    val entries by vm.investmentEntries.collectAsState()
    var open by remember { mutableStateOf<Investment?>(null) }

    if (invs.isEmpty()) EmptyNote("Track stocks, T-bills, fixed deposits, real estate, a stake in a business — what you put in and what it's worth now.")

    val byCur = Calc.sumByCurrency(invs.map { it.currency to it.currentValue })
    if (byCur.isNotEmpty()) {
        SectionLabel("Current value")
        byCur.forEach { (code, amt) -> AmountText(amt, code, size = 24) }
    }

    invs.forEach { i ->
        val put = Calc.invested(i, entries)
        val gain = i.currentValue - put
        VCard(Modifier.fillMaxWidth(), onClick = { open = i }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(i.name, color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Text(i.type, color = c.muted, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    AmountText(i.currentValue, i.currency, size = 15)
                    AmountText(gain, i.currency, size = 12, signed = true, color = if (gain >= 0) c.inc else c.exp)
                }
            }
        }
    }

    val openInv = open?.let { i -> invs.firstOrNull { it.id == i.id } }
    if (openInv != null) InvestmentDetailDialog(vm, openInv) { open = null }
}

@Composable
private fun InvestmentAddDialog(vm: VaultViewModel, businessId: Long?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(investmentTypes.first()) }
    var initial by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(vm.prefs.defaultCurrency.value) }
    var pickCur by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("New investment", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(name, { name = it }, "Name, e.g. MTN shares")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    investmentTypes.forEach { t -> VChip(t, selected = t == type) { type = t } }
                }
                Field(initial, { initial = it }, "Amount put in", number = true)
                VChip("$currency ▾") { pickCur = true }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = Money.parse(initial) ?: 0L
                if (name.isBlank()) ctx.toast("Give it a name")
                else {
                    vm.addInvestment(
                        Investment(name = name.trim(), type = type, currency = currency, currentValue = 0L, businessId = businessId),
                        a
                    )
                    onClose()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }
    )
    if (pickCur) CurrencyPickerDialog(currency, { currency = it; pickCur = false }) { pickCur = false }
}

@Composable
private fun InvestmentDetailDialog(vm: VaultViewModel, i: Investment, onClose: () -> Unit) {
    val c = LocalVault.current
    val ctx = LocalContext.current
    val entries by vm.investmentEntries.collectAsState()
    val mine = entries.filter { it.investmentId == i.id }
    val put = mine.sumOf { it.amount }
    val gain = i.currentValue - put
    var amount by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    fun parsed(): Long? = Money.parse(amount)?.takeIf { it > 0 }.also { if (it == null) ctx.toast("Enter an amount") }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(i.name, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(i.type, color = c.muted, fontSize = 13.sp)
                AmountText(i.currentValue, i.currency, size = 22, prefix = "Worth ")
                AmountText(put, i.currency, size = 13, prefix = "Put in ", color = c.muted, weight = FontWeight.Normal)
                AmountText(gain, i.currency, size = 13, signed = true, prefix = "Gain ", color = if (gain >= 0) c.inc else c.exp)
                Text("Value updated ${Dates.fmt(i.valueUpdatedAt)}", color = c.muted, fontSize = 12.sp)
                Field(amount, { amount = it }, "Amount", number = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VChip("Add money", selected = true, modifier = Modifier.weight(1f)) {
                        parsed()?.let { vm.investmentMove(i, it, "Added"); amount = "" }
                    }
                    VChip("Withdraw", modifier = Modifier.weight(1f)) {
                        parsed()?.let { vm.investmentMove(i, -it, "Withdrawn"); amount = "" }
                    }
                }
                VChip("Set as current value", modifier = Modifier.fillMaxWidth()) {
                    parsed()?.let { vm.setInvestmentValue(i, it); amount = "" }
                }
                TextButton(onClick = { confirmDelete = true }) { Text("Delete investment", color = c.exp) }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } }
    )
    if (confirmDelete) ConfirmDialog("Delete \"${i.name}\"?", "Its history will be removed too.", onDismiss = { confirmDelete = false }) {
        vm.deleteInvestment(i); onClose()
    }
}

// ======================= Debts =======================

@Composable
private fun DebtsTab(vm: VaultViewModel) {
    val c = LocalVault.current
    val debts by vm.scopedDebts.collectAsState()
    val payments by vm.debtPayments.collectAsState()
    var open by remember { mutableStateOf<Debt?>(null) }

    if (debts.isEmpty()) EmptyNote("Record money people owe you and money you owe — then log each repayment until it's cleared.")

    listOf(Direction.OWED_TO_ME to "They owe me", Direction.I_OWE to "I owe").forEach { (dir, label) ->
        val list = debts.filter { it.direction == dir }
        if (list.isNotEmpty()) {
            val totals = Calc.sumByCurrency(list.map { it.currency to Calc.remaining(it, payments) })
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(label, Modifier.weight(1f))
                totals.forEach { (code, amt) ->
                    AmountText(amt, code, size = 13, color = if (dir == Direction.OWED_TO_ME) c.inc else c.exp)
                }
            }
            list.forEach { d ->
                val left = Calc.remaining(d, payments)
                VCard(Modifier.fillMaxWidth(), onClick = { open = d }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.person, color = if (d.settled) c.muted else c.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text(
                                when {
                                    d.settled -> "Settled"
                                    d.dueDate != null -> "Due ${Dates.fmt(d.dueDate)}"
                                    else -> "No due date"
                                },
                                color = c.muted, fontSize = 12.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            AmountText(left, d.currency, size = 15)
                            AmountText(d.amount, d.currency, size = 11, prefix = "of ", color = c.muted, weight = FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }

    val openDebt = open?.let { d -> debts.firstOrNull { it.id == d.id } }
    if (openDebt != null) DebtDetailDialog(vm, openDebt) { open = null }
}

@Composable
private fun DebtAddDialog(vm: VaultViewModel, businessId: Long?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var dir by remember { mutableStateOf(0) }
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(vm.prefs.defaultCurrency.value) }
    var due by remember { mutableStateOf<Long?>(null) }
    var pickCur by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("New debt", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Segmented(listOf("They owe me", "I owe"), dir) { dir = it }
                Field(person, { person = it }, if (dir == 0) "Who owes you?" else "Who do you owe?")
                Field(amount, { amount = it }, "Amount", number = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VChip("$currency ▾") { pickCur = true }
                    VChip(due?.let { "Due ${Dates.fmt(it)}" } ?: "Due date (optional)") { pickDate = true }
                }
                Field(note, { note = it }, "Note (optional)")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = Money.parse(amount)
                when {
                    person.isBlank() -> ctx.toast("Enter a name")
                    a == null || a <= 0 -> ctx.toast("Enter an amount")
                    else -> {
                        vm.saveDebt(
                            Debt(
                                person = person.trim(),
                                direction = if (dir == 0) Direction.OWED_TO_ME else Direction.I_OWE,
                                amount = a, currency = currency, dueDate = due, note = note.trim(),
                                businessId = businessId
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
    if (pickDate) DatePickDialog(due ?: System.currentTimeMillis(), { due = Dates.startOfDay(it) }) { pickDate = false }
}

@Composable
private fun DebtDetailDialog(vm: VaultViewModel, d: Debt, onClose: () -> Unit) {
    val c = LocalVault.current
    val ctx = LocalContext.current
    val payments by vm.debtPayments.collectAsState()
    val mine = payments.filter { it.debtId == d.id }
    val left = Calc.remaining(d, payments)
    var amount by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(d.person, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (d.direction == Direction.OWED_TO_ME) "Owes you" else "You owe", color = c.muted)
                AmountText(left, d.currency, size = 22, prefix = "Left: ")
                AmountText(d.amount, d.currency, size = 13, prefix = "Original: ", color = c.muted, weight = FontWeight.Normal)
                d.dueDate?.let { Text("Due ${Dates.fmt(it)}", color = c.muted, fontSize = 13.sp) }
                if (d.note.isNotBlank()) Text(d.note, color = c.soft, fontSize = 13.sp)
                if (!d.settled) {
                    Field(amount, { amount = it }, "Payment amount", number = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VChip("Record payment", selected = true, modifier = Modifier.weight(1f)) {
                            val a = Money.parse(amount)
                            if (a == null || a <= 0) ctx.toast("Enter an amount")
                            else { vm.recordPayment(d, a, ""); amount = "" }
                        }
                        VChip("Mark settled", modifier = Modifier.weight(1f)) { vm.setSettled(d, true) }
                    }
                } else {
                    VChip("Reopen", modifier = Modifier.fillMaxWidth()) { vm.setSettled(d, false) }
                }
                if (mine.isNotEmpty()) {
                    SectionLabel("Payments")
                    mine.take(6).forEach { p ->
                        Row {
                            Text(Dates.fmt(p.date), color = c.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            AmountText(p.amount, d.currency, size = 12)
                        }
                    }
                }
                TextButton(onClick = { confirmDelete = true }) { Text("Delete debt", color = c.exp) }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } }
    )
    if (confirmDelete) ConfirmDialog("Delete this debt?", "Its payment history will be removed too.", onDismiss = { confirmDelete = false }) {
        vm.deleteDebt(d); onClose()
    }
}
