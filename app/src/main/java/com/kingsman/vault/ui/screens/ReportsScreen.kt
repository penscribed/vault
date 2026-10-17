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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingsman.vault.MainActivity
import com.kingsman.vault.data.Direction
import com.kingsman.vault.ui.AmountText
import com.kingsman.vault.ui.DatePickDialog
import com.kingsman.vault.ui.IconSquare
import com.kingsman.vault.ui.PrimaryButton
import com.kingsman.vault.ui.ProgressLine
import com.kingsman.vault.ui.ScreenHeader
import com.kingsman.vault.ui.SectionLabel
import com.kingsman.vault.ui.VCard
import com.kingsman.vault.ui.VChip
import com.kingsman.vault.ui.VaultViewModel
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.ui.toast
import com.kingsman.vault.util.Dates
import com.kingsman.vault.util.ReportBuilder
import com.kingsman.vault.util.ReportData
import java.time.LocalDate

@Composable
fun ReportsScreen(vm: VaultViewModel, activity: MainActivity) {
    val c = LocalVault.current
    val ctx = LocalContext.current
    val period by vm.reportPeriod.collectAsState()
    val txns by vm.reportTxns.collectAsState()
    val defaultCur by vm.prefs.defaultCurrency.collectAsState()
    val currencies by vm.txnCurrencies.collectAsState()
    val goals by vm.scopedGoals.collectAsState()
    val goalEntries by vm.goalEntries.collectAsState()
    val invs by vm.scopedInvestments.collectAsState()
    val invEntries by vm.investmentEntries.collectAsState()
    val debts by vm.scopedDebts.collectAsState()
    val payments by vm.debtPayments.collectAsState()
    val owed by vm.owedByBusinesses.collectAsState()
    val scope by vm.scope.collectAsState()
    val biz by vm.currentBusiness.collectAsState()

    var currency by remember { mutableStateOf(defaultCur) }
    var periodIdx by remember { mutableStateOf(0) }
    var pickFrom by remember { mutableStateOf(false) }
    var pickTo by remember { mutableStateOf(false) }
    var customFrom by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var exporting by remember { mutableStateOf(false) }

    val r: ReportData = ReportBuilder.build(
        scopeName = biz?.name ?: "Personal",
        currency = currency,
        periodLabel = period.label,
        from = period.from,
        to = period.to,
        txns = txns,
        goals = goals,
        goalEntries = goalEntries,
        investments = invs,
        investmentEntries = invEntries,
        debts = debts,
        debtPayments = payments,
        owedByBusinesses = if (scope == null) owed else emptyList()
    )

    fun export() {
        if (exporting) return
        exporting = true
        vm.exportPdf(r) { file, err ->
            exporting = false
            if (file != null) {
                activity.shareFile(file, "application/pdf", "Vault report — ${r.scopeName} — ${r.periodLabel}", "My Vault report")
            } else {
                ctx.toast("Couldn't create PDF: ${err ?: "unknown error"}")
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Reports") {
            IconSquare(Icons.Rounded.Share, "Export PDF") { export() }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VChip("This month", periodIdx == 0) { periodIdx = 0; vm.reportPeriod.value = vm.thisMonth() }
                VChip("Last month", periodIdx == 1) { periodIdx = 1; vm.reportPeriod.value = vm.lastMonth() }
                VChip("This year", periodIdx == 2) { periodIdx = 2; vm.reportPeriod.value = vm.thisYear() }
                VChip("Custom…", periodIdx == 3) { pickFrom = true }
            }
            val curOptions = (listOf(defaultCur) + currencies + goals.map { it.currency } + invs.map { it.currency }).distinct()
            if (curOptions.size > 1) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    curOptions.forEach { code -> VChip(code, code == currency) { currency = code } }
                }
            }
            Text(
                "${r.scopeName} · ${period.label}", color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )

            // Summary
            VCard(Modifier.fillMaxWidth()) {
                Row {
                    Stat("Income", r.totals.income, currency, c.inc, Modifier.weight(1f))
                    Stat("Expenses", r.totals.expense, currency, c.exp, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row {
                    Stat("Net", r.totals.net, currency, c.text, Modifier.weight(1f), signed = true)
                    Column(Modifier.weight(1f)) {
                        Text("Kept", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${r.savingsRate}%", color = c.accText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            CategoryBars("Expenses by category", r.expenseByCat, r.totals.expense, currency, c.exp)
            CategoryBars("Income by category", r.incomeByCat, r.totals.income, currency, c.inc)

            if (r.monthly.isNotEmpty()) {
                SectionLabel("Month by month")
                VCard(Modifier.fillMaxWidth()) {
                    r.monthly.forEach { (ym, inc, exp) ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(Dates.fmtMonShort(ym) + " " + ym.year, color = c.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            AmountText(inc, currency, size = 12, compact = true, color = c.inc, modifier = Modifier.weight(1f))
                            AmountText(exp, currency, size = 12, compact = true, color = c.exp, modifier = Modifier.weight(1f))
                            AmountText(inc - exp, currency, size = 12, compact = true, signed = true)
                        }
                    }
                }
            }

            SectionLabel("Wealth")
            VCard(Modifier.fillMaxWidth()) {
                Line("Total saved in goals", r.savedTotal, currency)
                Line("Saved this period", r.savedInPeriod, currency, signed = true)
                Line("Investments — value", r.investmentValue, currency)
                Line("Investments — put in", r.investedTotal, currency)
                Line("Owed to me", r.owedToMe, currency, color = c.inc)
                Line("I owe", r.iOwe, currency, color = c.exp)
                if (scope == null && r.owedByBusinesses > 0) Line("Businesses owe me", r.owedByBusinesses, currency, color = c.accText)
            }
            if (r.debts.isNotEmpty()) {
                SectionLabel("Open debts")
                VCard(Modifier.fillMaxWidth()) {
                    r.debts.forEach {
                        Line(
                            it.person + if (it.direction == Direction.OWED_TO_ME) " (owes me)" else " (I owe)",
                            it.remaining, currency
                        )
                    }
                }
            }

            PrimaryButton(if (exporting) "Creating PDF…" else "Export PDF", Modifier.fillMaxWidth(), enabled = !exporting) { export() }
        }
    }

    if (pickFrom) {
        DatePickDialog(Dates.startOfDay(customFrom), onPick = {
            customFrom = it
            pickTo = true
        }, onDismiss = { pickFrom = false })
    }
    if (pickTo) {
        DatePickDialog(System.currentTimeMillis(), onPick = {
            periodIdx = 3
            vm.reportPeriod.value = vm.customPeriod(customFrom, it)
        }, onDismiss = { pickTo = false })
    }
}

@Composable
private fun Stat(label: String, amount: Long, cur: String, color: Color, modifier: Modifier, signed: Boolean = false) {
    val c = LocalVault.current
    Column(modifier) {
        Text(label, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        AmountText(amount, cur, size = 18, color = color, compact = true, signed = signed)
    }
}

@Composable
private fun Line(label: String, amount: Long, cur: String, signed: Boolean = false, color: Color? = null) {
    val c = LocalVault.current
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.soft, fontSize = 14.sp, modifier = Modifier.weight(1f))
        AmountText(amount, cur, size = 14, signed = signed, color = color ?: c.text)
    }
}

@Composable
private fun CategoryBars(title: String, list: List<Pair<String, Long>>, total: Long, cur: String, color: Color) {
    if (list.isEmpty()) return
    val c = LocalVault.current
    SectionLabel(title)
    VCard(Modifier.fillMaxWidth()) {
        list.forEach { (name, amt) ->
            val frac = if (total > 0) amt.toFloat() / total else 0f
            Column(Modifier.padding(vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("${(frac * 100).toInt()}%  ", color = c.muted, fontSize = 12.sp)
                    AmountText(amt, cur, size = 14)
                }
                Spacer(Modifier.height(4.dp))
                ProgressLine(frac, color = color)
            }
        }
    }
}
