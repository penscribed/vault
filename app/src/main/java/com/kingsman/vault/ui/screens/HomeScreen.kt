package com.kingsman.vault.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.navigation.NavHostController
import com.kingsman.vault.R
import com.kingsman.vault.data.Direction
import com.kingsman.vault.data.Frequency
import com.kingsman.vault.ui.AmountText
import com.kingsman.vault.ui.IconSquare
import com.kingsman.vault.ui.NO_BIZ
import com.kingsman.vault.ui.PrimaryButton
import com.kingsman.vault.ui.ProgressLine
import com.kingsman.vault.ui.Routes
import com.kingsman.vault.ui.Segmented
import com.kingsman.vault.ui.TextInputDialog
import com.kingsman.vault.ui.VCard
import com.kingsman.vault.ui.VChip
import com.kingsman.vault.ui.VaultViewModel
import com.kingsman.vault.ui.goTab
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.util.Calc
import com.kingsman.vault.util.Dates
import java.time.YearMonth

@Composable
fun HomeScreen(vm: VaultViewModel, nav: NavHostController) {
    val c = LocalVault.current
    val prefs = vm.prefs
    val isBiz by vm.isBusiness.collectAsState()
    val scope by vm.scope.collectAsState()
    val biz by vm.currentBusiness.collectAsState()
    val businesses by vm.businesses.collectAsState()
    val cur by prefs.defaultCurrency.collectAsState()
    val hide by prefs.hideAmounts.collectAsState()
    val month by vm.monthTxns.collectAsState()
    val trend by vm.trendTxns.collectAsState()
    val due by vm.dueVariable.collectAsState()
    val goals by vm.scopedGoals.collectAsState()
    val goalEntries by vm.goalEntries.collectAsState()
    val invs by vm.scopedInvestments.collectAsState()
    val invEntries by vm.investmentEntries.collectAsState()
    val debts by vm.scopedDebts.collectAsState()
    val payments by vm.debtPayments.collectAsState()
    val owed by vm.owedByBusinesses.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val budgets by vm.scopedBudgets.collectAsState()
    val update by prefs.availableUpdate.collectAsState()

    var showBizMenu by remember { mutableStateOf(false) }
    var showAddBiz by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ----- Header -----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.vault_logo), contentDescription = "Vault logo",
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
            )
            Spacer(Modifier.width(10.dp))
            Text("Vault", color = c.text, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            IconSquare(
                if (hide) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                if (hide) "Show amounts" else "Hide amounts"
            ) { prefs.setHideAmounts(!hide) }
            Spacer(Modifier.width(8.dp))
            IconSquare(
                if (c.dark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                "Switch light or dark mode"
            ) { prefs.setTheme(if (c.dark) "LIGHT" else "DARK") }
        }

        if (update.isNotBlank() &&
            com.kingsman.vault.update.Updater.compareVersions(update, com.kingsman.vault.BuildConfig.VERSION_NAME) > 0
        ) {
            VCard(
                Modifier.fillMaxWidth(), bg = c.accSoft, border = c.accLine, padding = 14.dp,
                onClick = { nav.navigate(Routes.UPDATES) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Vault $update is available", color = c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Your data stays as it is", color = c.soft, fontSize = 12.sp)
                    }
                    Text("Update ›", color = c.accText, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }

        Segmented(listOf("Personal", "Business"), if (isBiz) 1 else 0) { vm.setBusinessMode(it == 1) }

        if (isBiz && businesses.isNotEmpty()) {
            Box {
                VChip("${biz?.name ?: "Choose business"}  ▾", selected = true) { showBizMenu = true }
                DropdownMenu(expanded = showBizMenu, onDismissRequest = { showBizMenu = false }) {
                    businesses.forEach { b ->
                        DropdownMenuItem(text = { Text(b.name) }, onClick = { vm.selectBusiness(b.id); showBizMenu = false })
                    }
                    DropdownMenuItem(text = { Text("+ Add business") }, onClick = { showBizMenu = false; showAddBiz = true })
                }
            }
        }

        if (isBiz && scope == NO_BIZ) {
            VCard(Modifier.fillMaxWidth()) {
                Text("Add your first business", color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Each business keeps its own income, expenses, savings, investments and debts — separate from your personal money.",
                    color = c.muted, fontSize = 14.sp
                )
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Add business", Modifier.fillMaxWidth()) { showAddBiz = true }
            }
        } else {
            // ----- Net this month -----
            val t = Calc.totals(month, cur)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Net · ${Dates.fmtMonth(YearMonth.now())} · $cur",
                    color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                AmountText(t.net, cur, size = 38)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AmountText(t.income, cur, compact = true, prefix = "↓ In ", color = c.inc, size = 13)
                    AmountText(t.expense, cur, compact = true, prefix = "↑ Out ", color = c.exp, size = 13)
                }
                val others = Calc.totalsByCurrency(month).filterKeys { it != cur }
                if (others.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Also:", color = c.muted, fontSize = 12.sp)
                        others.forEach { (code, tot) ->
                            AmountText(tot.net, code, compact = true, signed = true, size = 12, color = c.soft)
                        }
                    }
                }
            }

            // ----- 6-month chart -----
            VCard(Modifier.fillMaxWidth()) {
                Row {
                    Text("Last 6 months", color = c.text, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("In vs Out", color = c.muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                val months = (5 downTo 0).map { YearMonth.now().minusMonths(it.toLong()) }
                val data = Calc.monthly(trend, cur, months)
                val max = (data.maxOfOrNull { maxOf(it.second, it.third) } ?: 0L).coerceAtLeast(1L)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    data.forEachIndexed { i, (ym, inc, exp) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                Modifier
                                    .height(90.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight((inc.toFloat() / max).coerceIn(0.02f, 1f))
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(c.acc)
                                )
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxHeight((exp.toFloat() / max).coerceIn(0.02f, 1f))
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(c.bar2)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                Dates.fmtMonShort(ym), fontSize = 11.sp,
                                color = if (i == 5) c.text else c.muted,
                                fontWeight = if (i == 5) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ----- Variable recurring due (e.g. commission) -----
            due.forEach { r ->
                VCard(Modifier.fillMaxWidth(), bg = c.accSoft, border = c.accLine, padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${r.title} due", color = c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "${Frequency.label(r.frequency)} · enter this amount · ${Dates.fmt(r.nextDate)}",
                                color = c.soft, fontSize = 12.sp
                            )
                        }
                        Text(
                            "Skip", color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.Button) { vm.skipRecurring(r) }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        )
                        Box(
                            Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.acc)
                                .clickable(role = Role.Button) { nav.navigate(Routes.add(recurringId = r.id)) }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Enter", color = c.accInk, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ----- Bank alerts waiting -----
            if (suggestions.isNotEmpty()) {
                VCard(
                    Modifier.fillMaxWidth(), bg = c.accSoft, border = c.accLine, padding = 14.dp,
                    onClick = { nav.navigate(Routes.BANK) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${suggestions.size} bank alert${if (suggestions.size == 1) "" else "s"} to review",
                                color = c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                            Text("Add them in one tap — no typing", color = c.soft, fontSize = 12.sp)
                        }
                        Text("Review ›", color = c.accText, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }

            // ----- Budgets (closest to the limit first) -----
            if (budgets.isNotEmpty()) {
                VCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = { nav.navigate(Routes.BUDGETS) }) {
                    Row {
                        Text("Budgets this month", color = c.text, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("See all ›", color = c.accText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    budgets
                        .map { b -> b to Calc.budgetSpent(b, month) }
                        .sortedByDescending { (b, spent) -> if (b.amount > 0) spent.toDouble() / b.amount else 0.0 }
                        .take(3)
                        .forEach { (b, spent) ->
                            val frac = if (b.amount > 0) spent.toFloat() / b.amount else 0f
                            val color = budgetColor(frac, b.alertPercent, c.acc, c.exp)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(b.categoryName, color = c.soft, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                Text("${(frac * 100).toInt()}%", color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            ProgressLine(frac, color = color)
                        }
                }
            }

            // ----- Businesses owe me (personal mode) -----
            if (!isBiz && owed.isNotEmpty()) {
                val byCur = Calc.sumByCurrency(owed.map { it.currency to it.amount })
                VCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Text("Your businesses owe you", color = c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Business expenses you paid from personal money (${owed.size} not reimbursed)",
                        color = c.muted, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    byCur.forEach { (code, amt) -> AmountText(amt, code, size = 18, color = c.accText) }
                }
            }

            // ----- Wealth tiles -----
            val saved = goals.filter { it.currency == cur }.sumOf { Calc.saved(it, goalEntries) }
            val invCur = invs.filter { it.currency == cur }
            val invValue = invCur.sumOf { it.currentValue }
            val invIn = invCur.sumOf { Calc.invested(it, invEntries) }
            val gainPct = if (invIn > 0) ((invValue - invIn) * 1000 / invIn) / 10.0 else 0.0
            val openDebts = debts.filter { it.currency == cur && !it.settled }
            val owedMe = openDebts.filter { it.direction == Direction.OWED_TO_ME }.sumOf { Calc.remaining(it, payments) }
            val iOwe = openDebts.filter { it.direction == Direction.I_OWE }.sumOf { Calc.remaining(it, payments) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("SAVED", saved, cur, "${goals.size} goals", c.accText, Modifier.weight(1f)) { nav.goTab(Routes.WEALTH) }
                Tile(
                    "INVESTED", invValue, cur,
                    (if (gainPct >= 0) "+" else "") + "$gainPct%",
                    if (gainPct >= 0) c.inc else c.exp, Modifier.weight(1f)
                ) { nav.goTab(Routes.WEALTH) }
                Tile(
                    "DEBTS", owedMe - iOwe, cur,
                    if (owedMe - iOwe >= 0) "net owed to you" else "net you owe",
                    c.muted, Modifier.weight(1f), signed = true
                ) { nav.goTab(Routes.WEALTH) }
            }

            // ----- Recent -----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent", color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(
                    "See all", color = c.accText, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier
                        .clickable(role = Role.Button) { nav.goTab(Routes.RECORDS) }
                        .padding(6.dp)
                )
            }
            if (trend.isEmpty()) {
                Text(
                    "Nothing here yet. Tap + to add your first income or expense.",
                    color = c.muted, fontSize = 14.sp
                )
            }
            trend.take(6).forEach { t ->
                TxnRow(t) { nav.navigate(Routes.add(txnId = t.id)) }
            }
        }
    }

    if (showAddBiz) {
        TextInputDialog(
            title = "New business",
            label = "Business name",
            confirm = "Add",
            onDismiss = { showAddBiz = false }
        ) { name ->
            vm.addBusiness(name)
            vm.setBusinessMode(true)
            showAddBiz = false
        }
    }
}

@Composable
private fun Tile(
    label: String,
    amount: Long,
    cur: String,
    sub: String,
    subColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    signed: Boolean = false,
    onClick: () -> Unit
) {
    val c = LocalVault.current
    VCard(modifier, onClick = onClick, padding = 12.dp) {
        Text(label, color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        AmountText(amount, cur, compact = true, signed = signed, size = 16)
        Spacer(Modifier.height(2.dp))
        Text(sub, color = subColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
