package com.kingsman.vault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kingsman.vault.data.Kind
import com.kingsman.vault.data.Txn
import com.kingsman.vault.ui.AmountText
import com.kingsman.vault.ui.ConfirmDialog
import com.kingsman.vault.ui.Routes
import com.kingsman.vault.ui.ScreenHeader
import com.kingsman.vault.ui.Segmented
import com.kingsman.vault.ui.VCard
import com.kingsman.vault.ui.VaultViewModel
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.util.Calc
import com.kingsman.vault.util.Dates
import com.kingsman.vault.util.Money

@Composable
fun TxnRow(t: Txn, onClick: () -> Unit) {
    val c = LocalVault.current
    val income = t.kind == Kind.INCOME
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            Modifier
                .size(40.dp)
                .clip(shape)
                .background(c.card)
                .border(1.dp, c.line, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                t.categoryName.take(1).uppercase(), color = if (income) c.inc else c.exp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.categoryName, color = c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            val sub = buildList {
                if (t.note.isNotBlank()) add(t.note)
                if (t.tag.isNotBlank()) add("#${t.tag}")
                if (t.paidPersonally) add(if (t.reimbursed) "Reimbursed" else "Paid personally")
                if (t.receiptPath != null) add("Receipt")
                add(Dates.dayLabel(t.date))
            }.joinToString(" · ")
            Text(sub, color = c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        AmountText(
            t.amount, t.currency, size = 15, prefix = if (income) "+" else "−",
            color = if (income) c.inc else c.text, weight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RecordsScreen(vm: VaultViewModel, nav: NavHostController) {
    val c = LocalVault.current
    val month by vm.recordsMonth.collectAsState()
    val txns by vm.recordsTxns.collectAsState()
    val cur by vm.prefs.defaultCurrency.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Txn?>(null) }
    var confirmDelete by remember { mutableStateOf<Txn?>(null) }
    var viewReceipt by remember { mutableStateOf<String?>(null) }

    val shown = txns.filter {
        (filter == 0 || (filter == 1 && it.kind == Kind.INCOME) || (filter == 2 && it.kind == Kind.EXPENSE)) &&
            (query.isBlank() || listOf(it.categoryName, it.note, it.tag).any { s -> s.contains(query.trim(), ignoreCase = true) })
    }
    val groups = shown.groupBy { Dates.toLocalDate(it.date) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Records")
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹", color = c.text, fontSize = 26.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClickLabel = "Previous month") { vm.recordsMonth.value = month.minusMonths(1) }
                        .padding(horizontal = 14.dp)
                )
                Text(
                    Dates.fmtMonth(month), color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    "›", color = c.text, fontSize = 26.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClickLabel = "Next month") { vm.recordsMonth.value = month.plusMonths(1) }
                        .padding(horizontal = 14.dp)
                )
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search category, note or tag") },
                modifier = Modifier.fillMaxWidth()
            )
            Segmented(listOf("All", "Income", "Expense"), filter) { filter = it }
            val t = Calc.totals(shown, cur)
            Row {
                AmountText(t.income, cur, prefix = "In ", color = c.inc, size = 13, modifier = Modifier.weight(1f))
                AmountText(t.expense, cur, prefix = "Out ", color = c.exp, size = 13, modifier = Modifier.weight(1f))
                AmountText(t.net, cur, prefix = "Net ", size = 13)
            }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 110.dp)
        ) {
            if (shown.isEmpty()) {
                item {
                    VCard(Modifier.fillMaxWidth()) {
                        Text("No entries for this month yet.", color = c.muted)
                    }
                }
            }
            groups.forEach { (day, list) ->
                item(key = "h$day") {
                    Text(
                        Dates.dayLabel(Dates.startOfDay(day)), color = c.muted, fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
                    )
                }
                items(list, key = { it.id }) { t -> TxnRow(t) { selected = t } }
            }
        }
    }

    selected?.let { t ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(t.categoryName, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        (if (t.kind == Kind.INCOME) "+" else "−") + Money.format(t.amount, t.currency),
                        fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
                    )
                    Text(Dates.fmt(t.date))
                    if (t.note.isNotBlank()) Text(t.note)
                    if (t.tag.isNotBlank()) Text("#${t.tag}")
                    if (t.paidPersonally) Text(if (t.reimbursed) "Paid personally · reimbursed" else "Paid personally · not yet reimbursed")
                    t.receiptPath?.let { name ->
                        TextButton(onClick = { viewReceipt = name }) { Text("View receipt photo") }
                    }
                    if (t.paidPersonally) {
                        TextButton(onClick = { vm.setReimbursed(t, !t.reimbursed); selected = null }) {
                            Text(if (t.reimbursed) "Mark as not reimbursed" else "Mark as reimbursed")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selected = null; nav.navigate(Routes.add(txnId = t.id)) }) { Text("Edit") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = t; selected = null }) { Text("Delete", color = c.exp) }
            }
        )
    }
    viewReceipt?.let { name -> ReceiptViewerDialog(vm, name) { viewReceipt = null } }
    confirmDelete?.let { t ->
        ConfirmDialog("Delete entry?", "This can't be undone.", onDismiss = { confirmDelete = null }) { vm.deleteTxn(t) }
    }
}
