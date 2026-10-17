package com.kingsman.vault.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingsman.vault.MainActivity
import com.kingsman.vault.data.Budget
import com.kingsman.vault.data.Kind
import com.kingsman.vault.data.Scope
import com.kingsman.vault.ui.AmountText
import com.kingsman.vault.ui.ConfirmDialog
import com.kingsman.vault.ui.CurrencyPickerDialog
import com.kingsman.vault.ui.EmptyNote
import com.kingsman.vault.ui.Field
import com.kingsman.vault.ui.NO_BIZ
import com.kingsman.vault.ui.PrimaryButton
import com.kingsman.vault.ui.ProgressLine
import com.kingsman.vault.ui.ScreenHeader
import com.kingsman.vault.ui.VCard
import com.kingsman.vault.ui.VChip
import com.kingsman.vault.ui.VaultViewModel
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.ui.toast
import com.kingsman.vault.util.Calc
import com.kingsman.vault.util.Dates
import com.kingsman.vault.util.Money
import java.time.YearMonth

/** Amber used for "getting close" in both themes. */
val WarnColor = Color(0xFFF59E0B)

fun budgetColor(frac: Float, alertPercent: Int, normal: Color, over: Color): Color = when {
    frac >= 1f -> over
    frac * 100 >= alertPercent -> WarnColor
    else -> normal
}

@Composable
fun BudgetsScreen(vm: VaultViewModel, activity: MainActivity, onBack: () -> Unit) {
    val c = LocalVault.current
    val budgets by vm.scopedBudgets.collectAsState()
    val month by vm.monthTxns.collectAsState()
    val scope by vm.scope.collectAsState()
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Budget?>(null) }

    val ym = YearMonth.now()
    val daysLeft = ym.lengthOfMonth() - Dates.today().dayOfMonth + 1

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Budgets", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "${Dates.fmtMonth(ym)} · $daysLeft day${if (daysLeft == 1) "" else "s"} left",
                color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
            if (scope == NO_BIZ) {
                EmptyNote("Add a business on the Home screen first.")
            } else {
                PrimaryButton("+ New budget", Modifier.fillMaxWidth()) {
                    activity.ensureNotificationPermission()
                    adding = true
                }
                if (budgets.isEmpty()) {
                    EmptyNote(
                        "Set a monthly limit for any category — or for all spending. " +
                            "Vault warns you (even with the app closed) when you reach your alert level and again if you go over."
                    )
                }
                budgets.forEach { b ->
                    val spent = Calc.budgetSpent(b, month)
                    val frac = if (b.amount > 0) spent.toFloat() / b.amount else 0f
                    val left = b.amount - spent
                    val color = budgetColor(frac, b.alertPercent, c.acc, c.exp)
                    VCard(Modifier.fillMaxWidth(), onClick = { editing = b }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(b.categoryName, color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Text("${(frac * 100).toInt()}%", color = color, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row {
                            AmountText(spent, b.currency, size = 14)
                            AmountText(b.amount, b.currency, size = 14, prefix = "  of ", color = c.muted, weight = FontWeight.Normal)
                        }
                        Spacer(Modifier.height(8.dp))
                        ProgressLine(frac, color = color)
                        Spacer(Modifier.height(6.dp))
                        if (left >= 0) {
                            Row {
                                AmountText(left, b.currency, size = 12, prefix = "Left: ", color = c.soft, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                AmountText(left / daysLeft.coerceAtLeast(1), b.currency, size = 12, prefix = "≈ ", color = c.muted, weight = FontWeight.Normal)
                                Text(" a day", color = c.muted, fontSize = 12.sp)
                            }
                        } else {
                            AmountText(-left, b.currency, size = 12, prefix = "Over by ", color = c.exp, weight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (adding) BudgetDialog(vm, null, scope.takeIf { it != NO_BIZ }) { adding = false }
    editing?.let { b -> BudgetDialog(vm, b, b.businessId) { editing = null } }
}

@Composable
private fun BudgetDialog(vm: VaultViewModel, existing: Budget?, businessId: Long?, onClose: () -> Unit) {
    val c = LocalVault.current
    val ctx = LocalContext.current
    val categories by vm.categories.collectAsState()
    val scope = if (businessId == null) Scope.PERSONAL else Scope.BUSINESS
    val cats = categories.filter { it.kind == Kind.EXPENSE && it.scope == scope }

    var catId by remember { mutableStateOf(existing?.categoryId) }
    var catName by remember { mutableStateOf(existing?.categoryName ?: "All spending") }
    var amount by remember { mutableStateOf(existing?.let { Money.toInput(it.amount) } ?: "") }
    var currency by remember { mutableStateOf(existing?.currency ?: vm.prefs.defaultCurrency.value) }
    var alert by remember { mutableStateOf(existing?.alertPercent ?: 80) }
    var menu by remember { mutableStateOf(false) }
    var pickCur by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (existing == null) "New monthly budget" else "Edit budget", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    VChip("$catName ▾", selected = true, modifier = Modifier.fillMaxWidth()) { menu = true }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("All spending") }, onClick = { catId = null; catName = "All spending"; menu = false })
                        cats.forEach { k ->
                            DropdownMenuItem(text = { Text(k.name) }, onClick = { catId = k.id; catName = k.name; menu = false })
                        }
                    }
                }
                Field(amount, { amount = it }, "Monthly limit", number = true)
                VChip("$currency ▾") { pickCur = true }
                Text("Warn me at", color = c.muted, fontSize = 13.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(50, 75, 80, 90).forEach { p -> VChip("$p%", alert == p) { alert = p } }
                }
                if (existing != null) TextButton(onClick = { confirmDelete = true }) { Text("Delete budget", color = c.exp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = Money.parse(amount)
                if (a == null || a <= 0) ctx.toast("Enter a monthly limit")
                else {
                    vm.saveBudget(
                        Budget(
                            id = existing?.id ?: 0L,
                            categoryId = catId, categoryName = catName, businessId = businessId,
                            currency = currency, amount = a, alertPercent = alert
                        )
                    )
                    onClose()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }
    )
    if (pickCur) CurrencyPickerDialog(currency, { currency = it; pickCur = false }) { pickCur = false }
    if (confirmDelete && existing != null) {
        ConfirmDialog("Delete this budget?", "Your entries are not affected.", onDismiss = { confirmDelete = false }) {
            vm.deleteBudget(existing); onClose()
        }
    }
}
