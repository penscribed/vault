package com.kingsman.vault.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kingsman.vault.VaultApp
import com.kingsman.vault.data.Budget
import com.kingsman.vault.data.Business
import com.kingsman.vault.data.Category
import com.kingsman.vault.data.Debt
import com.kingsman.vault.data.DebtPayment
import com.kingsman.vault.data.Goal
import com.kingsman.vault.data.GoalEntry
import com.kingsman.vault.data.Investment
import com.kingsman.vault.data.InvestmentEntry
import com.kingsman.vault.data.Recurring
import com.kingsman.vault.data.Scope
import com.kingsman.vault.data.SmsSuggestion
import com.kingsman.vault.data.SuggestionStatus
import com.kingsman.vault.data.Txn
import com.kingsman.vault.update.UpdateWorker
import com.kingsman.vault.update.Updater
import com.kingsman.vault.util.Calc
import com.kingsman.vault.util.Dates
import com.kingsman.vault.util.PdfReport
import com.kingsman.vault.util.ReportBuilder
import com.kingsman.vault.util.ReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/** Business mode with no business created yet: matches nothing. */
const val NO_BIZ = -1L

data class Period(val label: String, val from: Long, val to: Long)

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VaultApp
    val prefs = app.prefs
    private val repo get() = app.repo
    private val dao get() = app.repo.dao

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, initial)

    // ---------------- Reference data ----------------
    val businesses = dao.businesses().state(emptyList())
    val categories = dao.categories().state(emptyList())
    val usage: StateFlow<Map<Long, Int>> = dao.categoryUsage()
        .map { l -> l.mapNotNull { cc -> cc.categoryId?.let { it to cc.c } }.toMap() }
        .state(emptyMap())

    val isBusiness = prefs.mode.map { it == Scope.BUSINESS }.state(prefs.mode.value == Scope.BUSINESS)

    /** null = Personal mode; otherwise the selected business id (or NO_BIZ). */
    val scope: StateFlow<Long?> = combine(prefs.mode, prefs.businessId, businesses) { mode, bid, list ->
        if (mode != Scope.BUSINESS) null
        else list.firstOrNull { it.id == bid }?.id ?: list.firstOrNull()?.id ?: NO_BIZ
    }.state(null)

    val currentBusiness: StateFlow<Business?> = combine(scope, businesses) { s, l ->
        l.firstOrNull { it.id == s }
    }.state(null)

    fun scopeName(): String = currentBusiness.value?.name ?: "Personal"

    // ---------------- Everything else ----------------
    val goals = dao.goals().state(emptyList())
    val goalEntries = dao.goalEntries().state(emptyList())
    val investments = dao.investments().state(emptyList())
    val investmentEntries = dao.investmentEntries().state(emptyList())
    val debts = dao.debts().state(emptyList())
    val debtPayments = dao.debtPayments().state(emptyList())
    val recurring = dao.recurring().state(emptyList())
    val owedByBusinesses = dao.owedByBusinesses().state(emptyList())
    val txnCurrencies = dao.txnCurrencies().state(emptyList())

    val scopedGoals = combine(goals, scope) { l, s -> l.filter { it.businessId == s } }.state(emptyList())
    val scopedInvestments = combine(investments, scope) { l, s -> l.filter { it.businessId == s } }.state(emptyList())
    val scopedDebts = combine(debts, scope) { l, s -> l.filter { it.businessId == s } }.state(emptyList())
    val scopedRecurring = combine(recurring, scope) { l, s -> l.filter { it.businessId == s } }.state(emptyList())

    /** Variable-amount recurring entries (e.g. commission) waiting for you to enter the amount. */
    val dueVariable = combine(recurring, scope) { l, s ->
        val now = System.currentTimeMillis()
        l.filter { it.active && it.variable && it.businessId == s && it.nextDate <= now }
    }.state(emptyList())

    val budgets = dao.budgets().state(emptyList())
    val scopedBudgets = combine(budgets, scope) { l, s -> l.filter { it.businessId == s } }.state(emptyList())
    val suggestions = dao.pendingSuggestions().state(emptyList())

    /** One-off messages for a Toast (budget alerts etc.). */
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events

    private suspend fun runBudgetCheck() {
        runCatching { repo.checkBudgets() }.getOrNull()?.forEach { _events.tryEmit(it) }
    }

    // ---------------- Home ----------------
    val monthTxns = scope.flatMapLatest { s ->
        val (f, t) = Dates.monthRange(YearMonth.now())
        dao.txnsInRange(s, f, t)
    }.state(emptyList())

    val trendTxns = scope.flatMapLatest { s ->
        val now = YearMonth.now()
        dao.txnsInRange(s, Dates.monthRange(now.minusMonths(5)).first, Dates.monthRange(now).second)
    }.state(emptyList())

    // ---------------- Records ----------------
    val recordsMonth = MutableStateFlow(YearMonth.now())
    val recordsTxns = combine(scope, recordsMonth) { s, m -> s to m }
        .flatMapLatest { (s, m) ->
            val (f, t) = Dates.monthRange(m)
            dao.txnsInRange(s, f, t)
        }.state(emptyList())

    // ---------------- Reports ----------------
    val reportPeriod = MutableStateFlow(thisMonth())
    val reportTxns = combine(scope, reportPeriod) { s, p -> s to p }
        .flatMapLatest { (s, p) -> dao.txnsInRange(s, p.from, p.to) }
        .state(emptyList())

    fun thisMonth(): Period {
        val ym = YearMonth.now()
        val (f, t) = Dates.monthRange(ym)
        return Period(Dates.fmtMonth(ym), f, t)
    }

    fun lastMonth(): Period {
        val ym = YearMonth.now().minusMonths(1)
        val (f, t) = Dates.monthRange(ym)
        return Period(Dates.fmtMonth(ym), f, t)
    }

    fun thisYear(): Period {
        val y = LocalDate.now().year
        val (f, t) = Dates.yearRange(y)
        return Period("Year $y", f, t)
    }

    fun customPeriod(from: LocalDate, to: LocalDate): Period {
        val (a, b) = if (to.isBefore(from)) to to from else from to to
        val (f, t) = Dates.dayRange(a, b)
        return Period("${Dates.fmt(f)} – ${Dates.fmt(t - 1)}", f, t)
    }

    fun buildReport(currency: String): ReportData {
        val p = reportPeriod.value
        return ReportBuilder.build(
            scopeName = scopeName(),
            currency = currency,
            periodLabel = p.label,
            from = p.from,
            to = p.to,
            txns = reportTxns.value,
            goals = scopedGoals.value,
            goalEntries = goalEntries.value,
            investments = scopedInvestments.value,
            investmentEntries = investmentEntries.value,
            debts = scopedDebts.value,
            debtPayments = debtPayments.value,
            owedByBusinesses = if (scope.value == null) owedByBusinesses.value else emptyList()
        )
    }

    fun exportPdf(report: ReportData, onDone: (File?, String?) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { PdfReport.write(app, report) } }
            onDone(result.getOrNull(), result.exceptionOrNull()?.message)
        }
    }

    // ---------------- Mode & businesses ----------------
    fun setBusinessMode(on: Boolean) = prefs.setMode(if (on) Scope.BUSINESS else Scope.PERSONAL)
    fun selectBusiness(id: Long) = prefs.setBusinessId(id)

    fun addBusiness(name: String) = viewModelScope.launch {
        val id = dao.insertBusiness(Business(name = name))
        prefs.setBusinessId(id)
    }

    fun renameBusiness(b: Business, name: String) = viewModelScope.launch { dao.updateBusiness(b.copy(name = name)) }
    fun archiveBusiness(b: Business) = viewModelScope.launch { dao.updateBusiness(b.copy(archived = true)) }

    // ---------------- Categories ----------------
    fun addCategory(name: String, kind: String, scope: String, onCreated: (Category) -> Unit = {}) =
        viewModelScope.launch {
            val cat = Category(name = name, kind = kind, scope = scope)
            val id = dao.insertCategory(cat)
            onCreated(cat.copy(id = id))
        }

    fun renameCategory(c: Category, name: String) = viewModelScope.launch { dao.updateCategory(c.copy(name = name)) }
    fun archiveCategory(c: Category) = viewModelScope.launch { dao.updateCategory(c.copy(archived = true)) }

    // ---------------- Transactions ----------------
    /** [previousReceipt] = the receipt the entry had before editing, deleted if it was replaced/removed. */
    fun saveTxn(t: Txn, previousReceipt: String? = null) = viewModelScope.launch {
        if (t.id == 0L) dao.insertTxn(t) else dao.updateTxn(t)
        if (previousReceipt != null && previousReceipt != t.receiptPath) repo.receipts.delete(previousReceipt)
        runBudgetCheck()
    }

    fun deleteTxn(t: Txn) = viewModelScope.launch {
        dao.deleteTxn(t)
        repo.receipts.delete(t.receiptPath)
        runBudgetCheck()
    }

    // ---------------- Receipts ----------------
    fun captureFile(): File = repo.receipts.captureFile()
    suspend fun importReceipt(file: File): String? = runCatching { repo.receipts.importFromFile(file) }.getOrNull()
    suspend fun importReceipt(uri: Uri): String? = runCatching { repo.receipts.importFromUri(uri) }.getOrNull()
    suspend fun loadReceipt(name: String) = runCatching { repo.receipts.load(name) }.getOrNull()
    fun discardReceipt(name: String?) = repo.receipts.delete(name)

    // ---------------- Budgets ----------------
    fun saveBudget(b: Budget) = viewModelScope.launch {
        if (b.id == 0L) dao.insertBudget(b) else dao.updateBudget(b.copy(alertedMonth = "", alertedLevel = 0))
        runBudgetCheck()
    }

    fun deleteBudget(b: Budget) = viewModelScope.launch { dao.deleteBudget(b.id) }

    // ---------------- Bank alerts ----------------
    fun hasSmsPermission() = repo.hasSmsPermission()

    fun scanSms(days: Int, onDone: (Int) -> Unit = {}) = viewModelScope.launch {
        val since = System.currentTimeMillis() - days * 24L * 3600 * 1000
        onDone(runCatching { repo.scanInbox(since) }.getOrDefault(0))
    }

    /** New messages since the last scan (or the last 30 days the first time). */
    fun scanSmsSinceLast() = viewModelScope.launch {
        if (!prefs.smsEnabled.value || !repo.hasSmsPermission()) return@launch
        val last = prefs.lastSmsScan
        val since = if (last == 0L) System.currentTimeMillis() - 30L * 24 * 3600 * 1000 else last - 60_000
        runCatching { repo.scanInbox(since) }
    }

    fun addPastedAlert(text: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        onDone(runCatching { repo.ingestAlert("Pasted", text, System.currentTimeMillis(), notify = false) }.getOrDefault(false))
    }

    fun dismissSuggestion(s: SmsSuggestion) = viewModelScope.launch { dao.setSuggestionStatus(s.id, SuggestionStatus.DISMISSED) }
    fun dismissAllSuggestions() = viewModelScope.launch { dao.dismissAllSuggestions() }
    fun markSuggestionAdded(id: Long) = viewModelScope.launch { dao.setSuggestionStatus(id, SuggestionStatus.ADDED) }
    suspend fun getSuggestion(id: Long): SmsSuggestion? = dao.getSuggestion(id)

    fun onUnlocked() {
        refreshRecurring()
        scanSmsSinceLast()
        checkUpdatesQuietly()
    }

    // ---------------- App updates ----------------
    /** At most every 12 hours while you use the app (the daily background check covers the rest). */
    fun checkUpdatesQuietly() = viewModelScope.launch {
        if (!prefs.autoUpdate.value) return@launch
        if (System.currentTimeMillis() - prefs.lastUpdateCheck < 12L * 3600 * 1000) return@launch
        runCatching { Updater.checkForUpdate() }.onSuccess { r ->
            prefs.lastUpdateCheck = System.currentTimeMillis()
            prefs.setAvailableUpdate(r?.version ?: "")
        }
    }

    fun setAutoUpdate(on: Boolean) {
        prefs.setAutoUpdate(on)
        if (on) UpdateWorker.schedule(app) else UpdateWorker.cancel(app)
    }
    fun setReimbursed(t: Txn, v: Boolean) = viewModelScope.launch { dao.updateTxn(t.copy(reimbursed = v)) }
    suspend fun getTxn(id: Long): Txn? = dao.getTxn(id)

    // ---------------- Savings goals ----------------
    fun saveGoal(g: Goal) = viewModelScope.launch { if (g.id == 0L) dao.insertGoal(g) else dao.updateGoal(g) }

    fun goalMove(g: Goal, amount: Long, note: String) = viewModelScope.launch {
        dao.insertGoalEntry(GoalEntry(goalId = g.id, amount = amount, date = System.currentTimeMillis(), note = note))
    }

    fun deleteGoal(g: Goal) = viewModelScope.launch { dao.deleteGoalEntries(g.id); dao.deleteGoal(g.id) }

    // ---------------- Investments ----------------
    fun addInvestment(i: Investment, initial: Long) = viewModelScope.launch {
        val id = dao.insertInvestment(i.copy(currentValue = if (i.currentValue > 0) i.currentValue else initial))
        if (initial != 0L) dao.insertInvestmentEntry(
            InvestmentEntry(investmentId = id, amount = initial, date = System.currentTimeMillis(), note = "Initial")
        )
    }

    /** Positive = add money, negative = withdraw. Current value moves by the same amount. */
    fun investmentMove(i: Investment, amount: Long, note: String) = viewModelScope.launch {
        dao.insertInvestmentEntry(
            InvestmentEntry(investmentId = i.id, amount = amount, date = System.currentTimeMillis(), note = note)
        )
        dao.updateInvestment(i.copy(currentValue = (i.currentValue + amount).coerceAtLeast(0)))
    }

    fun setInvestmentValue(i: Investment, value: Long) = viewModelScope.launch {
        dao.updateInvestment(i.copy(currentValue = value, valueUpdatedAt = System.currentTimeMillis()))
    }

    fun deleteInvestment(i: Investment) = viewModelScope.launch {
        dao.deleteInvestmentEntries(i.id); dao.deleteInvestment(i.id)
    }

    // ---------------- Debts ----------------
    fun saveDebt(d: Debt) = viewModelScope.launch { if (d.id == 0L) dao.insertDebt(d) else dao.updateDebt(d) }

    fun recordPayment(d: Debt, amount: Long, note: String) = viewModelScope.launch {
        dao.insertDebtPayment(DebtPayment(debtId = d.id, amount = amount, date = System.currentTimeMillis(), note = note))
        val remaining = d.amount - Calc.paid(d, debtPayments.value) - amount
        if (remaining <= 0) dao.updateDebt(d.copy(settled = true))
    }

    fun setSettled(d: Debt, v: Boolean) = viewModelScope.launch { dao.updateDebt(d.copy(settled = v)) }
    fun deleteDebt(d: Debt) = viewModelScope.launch { dao.deleteDebtPayments(d.id); dao.deleteDebt(d.id) }

    // ---------------- Recurring & subscriptions ----------------
    fun saveRecurring(r: Recurring) = viewModelScope.launch {
        if (r.id == 0L) dao.insertRecurring(r) else dao.updateRecurring(r)
        repo.processRecurring()
    }

    fun deleteRecurring(r: Recurring) = viewModelScope.launch { dao.deleteRecurring(r.id) }
    fun setRecurringActive(r: Recurring, v: Boolean) = viewModelScope.launch { dao.updateRecurring(r.copy(active = v)) }
    fun confirmRecurring(r: Recurring, txn: Txn) = viewModelScope.launch {
        repo.confirmRecurring(r, txn)
        runBudgetCheck()
    }
    fun skipRecurring(r: Recurring) = viewModelScope.launch { repo.skipRecurring(r) }
    suspend fun getRecurring(id: Long): Recurring? = dao.getRecurring(id)
    fun refreshRecurring() = viewModelScope.launch {
        runCatching { repo.processRecurring() }
        runBudgetCheck()
    }

    // ---------------- Backup ----------------
    fun exportBackup(password: String, includeReceipts: Boolean, onDone: (File?, String?) -> Unit) = viewModelScope.launch {
        val r = runCatching { repo.exportBackup(password, includeReceipts) }
        onDone(r.getOrNull(), r.exceptionOrNull()?.message)
    }

    fun importBackup(uri: Uri, password: String, onDone: (String?) -> Unit) = viewModelScope.launch {
        val r = runCatching { repo.importBackup(uri, password) }
        onDone(
            r.exceptionOrNull()?.let {
                if (it is javax.crypto.AEADBadTagException) "Wrong password for this backup"
                else it.message ?: "Restore failed"
            }
        )
    }

    init {
        viewModelScope.launch {
            repo.seedIfNeeded()
            runCatching { repo.processRecurring() }
        }
    }
}
