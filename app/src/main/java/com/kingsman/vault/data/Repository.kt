package com.kingsman.vault.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.google.gson.Gson
import com.kingsman.vault.security.SecurityManager
import com.kingsman.vault.util.BankSmsParser
import com.kingsman.vault.util.Dates
import com.kingsman.vault.util.Money
import com.kingsman.vault.util.Notifier
import com.kingsman.vault.util.ReceiptStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.YearMonth

const val ROUTE_BUDGETS = "budgets"
const val ROUTE_BANK = "bankalerts"

data class BackupData(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    // Nullable because Gson can leave fields null when reading older/other files.
    val defaultCurrency: String? = null,
    val businesses: List<Business>? = null,
    val categories: List<Category>? = null,
    val transactions: List<Txn>? = null,
    val goals: List<Goal>? = null,
    val goalEntries: List<GoalEntry>? = null,
    val investments: List<Investment>? = null,
    val investmentEntries: List<InvestmentEntry>? = null,
    val debts: List<Debt>? = null,
    val debtPayments: List<DebtPayment>? = null,
    val recurring: List<Recurring>? = null,
    val budgets: List<Budget>? = null,
    /** Receipt file name -> base64 JPEG (only when receipts are included). */
    val receipts: Map<String, String>? = null
)

class Repository(
    private val context: Context,
    private val db: VaultDatabase,
    private val prefs: Prefs,
    private val security: SecurityManager,
    val receipts: ReceiptStore
) {
    val dao: VaultDao = db.dao()

    // ---------------- First run ----------------
    suspend fun seedIfNeeded() {
        if (prefs.seeded || dao.categoryCount() > 0) {
            prefs.seeded = true
            return
        }
        val list = mutableListOf<Category>()
        fun add(scope: String, kind: String, vararg names: String) =
            names.forEach { list += Category(name = it, kind = kind, scope = scope) }

        add(
            Scope.PERSONAL, Kind.EXPENSE,
            "Food & groceries", "Transport & fuel", "Rent & housing", "Utilities", "Data & airtime",
            "Health", "Family support", "Giving & tithes", "Education", "Shopping",
            "Entertainment", "Travel", "Subscriptions", "Personal care", "Other"
        )
        add(
            Scope.PERSONAL, Kind.INCOME,
            "Salary", "Commission", "Business payout", "Investment returns", "Gift", "Other income"
        )
        add(
            Scope.BUSINESS, Kind.EXPENSE,
            "Stock purchase", "Salaries & wages", "Rent", "Diesel & power", "Utilities", "Logistics",
            "Repairs & maintenance", "Marketing", "Tax & levies", "Bank charges", "Equipment", "Other"
        )
        add(Scope.BUSINESS, Kind.INCOME, "Sales", "Services", "Other income")
        dao.insertCategories(list)
        prefs.seeded = true
    }

    // ---------------- Recurring ----------------
    /** Logs every fixed-amount recurring entry that has fallen due. Variable ones wait for you. */
    suspend fun processRecurring(now: Long = System.currentTimeMillis()) {
        for (r in dao.activeRecurring()) {
            if (r.variable) continue
            var next = r.nextDate
            var guard = 0
            while (next <= now && guard < 400) {
                dao.insertTxn(
                    Txn(
                        kind = r.kind, amount = r.amount, currency = r.currency,
                        categoryId = r.categoryId, categoryName = r.categoryName,
                        businessId = r.businessId, date = next,
                        note = r.title, recurringId = r.id
                    )
                )
                next = Dates.advance(next, r.frequency)
                guard++
            }
            if (next != r.nextDate) dao.updateRecurring(r.copy(nextDate = next))
        }
    }

    suspend fun confirmRecurring(r: Recurring, txn: Txn) {
        db.withTransaction {
            dao.insertTxn(txn.copy(recurringId = r.id))
            dao.updateRecurring(r.copy(nextDate = Dates.advance(r.nextDate, r.frequency)))
        }
    }

    suspend fun skipRecurring(r: Recurring) {
        dao.updateRecurring(r.copy(nextDate = Dates.advance(r.nextDate, r.frequency)))
    }

    // ---------------- Budgets ----------------
    /**
     * Compares this month's spending with every budget and sends a notification the first time
     * a budget passes its alert % and again when it goes over 100%. Returns in-app messages.
     */
    suspend fun checkBudgets(): List<String> {
        val ym = YearMonth.now()
        val key = ym.toString()
        val (from, to) = Dates.monthRange(ym)
        val messages = mutableListOf<String>()
        for (b in dao.budgetList()) {
            if (b.amount <= 0) continue
            val spent = dao.spent(b.businessId, b.categoryId, b.currency, from, to)
            val pct = (spent * 100 / b.amount).toInt()
            val level = when {
                pct >= 100 -> 2
                pct >= b.alertPercent -> 1
                else -> 0
            }
            val previous = if (b.alertedMonth == key) b.alertedLevel else 0
            if (level > previous) {
                val title = if (level == 2) "Over budget: ${b.categoryName}" else "Budget alert: ${b.categoryName}"
                val text = "You've used $pct% (${Money.format(spent, b.currency)} of " +
                    "${Money.format(b.amount, b.currency)}) this month."
                Notifier.show(context, Notifier.CH_BUDGET, (1000 + b.id % 1000).toInt(), title, text, "Budget alert", ROUTE_BUDGETS)
                messages += "$title · $pct% used"
                dao.updateBudget(b.copy(alertedMonth = key, alertedLevel = level))
            } else if (b.alertedMonth == key && level < previous) {
                dao.updateBudget(b.copy(alertedLevel = level))
            }
        }
        return messages
    }

    // ---------------- Bank alerts ----------------
    /** Turns a bank SMS / pasted alert into a pending suggestion. Returns true if it's new. */
    suspend fun ingestAlert(sender: String, body: String, date: Long, notify: Boolean): Boolean {
        val p = BankSmsParser.parse(body, sender, prefs.defaultCurrency.value) ?: return false
        val id = dao.insertSuggestion(
            SmsSuggestion(
                hash = BankSmsParser.hash(sender, body),
                sender = sender.take(40),
                body = body.take(600),
                kind = p.kind,
                amount = p.amount,
                currency = p.currency,
                description = p.description,
                date = date
            )
        )
        if (id > 0 && notify) {
            Notifier.show(
                context, Notifier.CH_BANK, (2000 + id % 1000).toInt(),
                (if (p.kind == Kind.INCOME) "Money in: " else "Money out: ") + Money.format(p.amount, p.currency),
                "${p.description} — tap to add it to Vault",
                "New bank alert to review",
                ROUTE_BANK
            )
        }
        return id > 0
    }

    fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    /** Reads bank alerts from the SMS inbox newer than [sinceMs]. Returns how many new ones were found. */
    suspend fun scanInbox(sinceMs: Long): Int = withContext(Dispatchers.IO) {
        if (!hasSmsPermission()) return@withContext 0
        var found = 0
        val cursor = runCatching {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} > ?",
                arrayOf(sinceMs.toString()),
                "${Telephony.Sms.DATE} DESC"
            )
        }.getOrNull()
        cursor?.use {
            var read = 0
            while (it.moveToNext() && read < 1500) {
                read++
                val sender = it.getString(0) ?: ""
                val body = it.getString(1) ?: continue
                val date = it.getLong(2)
                if (ingestAlert(sender, body, date, notify = false)) found++
            }
        }
        prefs.lastSmsScan = System.currentTimeMillis()
        found
    }

    // ---------------- Backup ----------------
    suspend fun exportBackup(password: String, includeReceipts: Boolean = true): File = withContext(Dispatchers.IO) {
        val txns = dao.allTxns()
        val receiptMap = if (!includeReceipts) null else txns.mapNotNull { t ->
            val name = t.receiptPath ?: return@mapNotNull null
            receipts.rawJpeg(name)?.let { name to Base64.encodeToString(it, Base64.NO_WRAP) }
        }.toMap()
        val data = BackupData(
            defaultCurrency = prefs.defaultCurrency.value,
            businesses = dao.allBusinesses(),
            categories = dao.allCategories(),
            transactions = if (includeReceipts) txns else txns.map { it.copy(receiptPath = null) },
            goals = dao.allGoals(),
            goalEntries = dao.allGoalEntries(),
            investments = dao.allInvestments(),
            investmentEntries = dao.allInvestmentEntries(),
            debts = dao.allDebts(),
            debtPayments = dao.allDebtPayments(),
            recurring = dao.allRecurring(),
            budgets = dao.allBudgets(),
            receipts = receiptMap
        )
        val json = Gson().toJson(data).toByteArray(Charsets.UTF_8)
        val encrypted = security.encryptBackup(json, password)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.filter { it.name.endsWith(".vault") }?.forEach { it.delete() }
        val file = File(dir, "Vault-backup-${Dates.stamp()}.vault")
        file.writeBytes(encrypted)
        prefs.setLastBackup(System.currentTimeMillis())
        file
    }

    /** Replaces ALL data with the backup's contents. Throws on wrong password / bad file. */
    suspend fun importBackup(uri: Uri, password: String) = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not open the file")
        val json = String(security.decryptBackup(bytes, password), Charsets.UTF_8)
        val d = Gson().fromJson(json, BackupData::class.java)
        db.withTransaction {
            dao.clearTxns(); dao.clearGoalEntries(); dao.clearGoals()
            dao.clearInvestmentEntries(); dao.clearInvestments()
            dao.clearDebtPayments(); dao.clearDebts(); dao.clearRecurring()
            dao.clearCategories(); dao.clearBusinesses(); dao.clearBudgets()
            dao.putBusinesses(d.businesses.orEmpty())
            dao.putCategories(d.categories.orEmpty())
            dao.putTxns(d.transactions.orEmpty())
            dao.putGoals(d.goals.orEmpty())
            dao.putGoalEntries(d.goalEntries.orEmpty())
            dao.putInvestments(d.investments.orEmpty())
            dao.putInvestmentEntries(d.investmentEntries.orEmpty())
            dao.putDebts(d.debts.orEmpty())
            dao.putDebtPayments(d.debtPayments.orEmpty())
            dao.putRecurring(d.recurring.orEmpty())
            dao.putBudgets(d.budgets.orEmpty())
        }
        receipts.deleteAll()
        d.receipts.orEmpty().forEach { (name, b64) ->
            runCatching { receipts.restoreJpeg(name, Base64.decode(b64, Base64.NO_WRAP)) }
        }
        d.defaultCurrency?.let { prefs.setDefaultCurrency(it) }
        prefs.seeded = true
    }
}
