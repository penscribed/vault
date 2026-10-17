package com.kingsman.vault.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    // ---------- Businesses ----------
    @Query("SELECT * FROM businesses WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun businesses(): Flow<List<Business>>

    @Insert
    suspend fun insertBusiness(b: Business): Long

    @Update
    suspend fun updateBusiness(b: Business)

    // ---------- Categories ----------
    @Query("SELECT * FROM categories WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun categories(): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Insert
    suspend fun insertCategory(c: Category): Long

    @Insert
    suspend fun insertCategories(c: List<Category>)

    @Update
    suspend fun updateCategory(c: Category)

    @Query("SELECT categoryId, COUNT(*) AS c FROM transactions GROUP BY categoryId")
    fun categoryUsage(): Flow<List<CatCount>>

    // ---------- Transactions ----------
    @Insert
    suspend fun insertTxn(t: Txn): Long

    @Update
    suspend fun updateTxn(t: Txn)

    @Delete
    suspend fun deleteTxn(t: Txn)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTxn(id: Long): Txn?

    @Query(
        "SELECT * FROM transactions WHERE ((:bid IS NULL AND businessId IS NULL) OR businessId = :bid) " +
            "AND date >= :from AND date < :to ORDER BY date DESC, id DESC"
    )
    fun txnsInRange(bid: Long?, from: Long, to: Long): Flow<List<Txn>>

    @Query(
        "SELECT * FROM transactions WHERE businessId IS NOT NULL AND paidPersonally = 1 " +
            "AND reimbursed = 0 AND kind = 'EXPENSE' ORDER BY date DESC"
    )
    fun owedByBusinesses(): Flow<List<Txn>>

    @Query("SELECT DISTINCT currency FROM transactions")
    fun txnCurrencies(): Flow<List<String>>

    // ---------- Savings goals ----------
    @Query("SELECT * FROM savings_goals WHERE archived = 0 ORDER BY createdAt")
    fun goals(): Flow<List<Goal>>

    @Query("SELECT * FROM goal_entries ORDER BY date DESC, id DESC")
    fun goalEntries(): Flow<List<GoalEntry>>

    @Insert
    suspend fun insertGoal(g: Goal): Long

    @Update
    suspend fun updateGoal(g: Goal)

    @Insert
    suspend fun insertGoalEntry(e: GoalEntry): Long

    @Query("DELETE FROM goal_entries WHERE goalId = :goalId")
    suspend fun deleteGoalEntries(goalId: Long)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    // ---------- Investments ----------
    @Query("SELECT * FROM investments WHERE archived = 0 ORDER BY createdAt")
    fun investments(): Flow<List<Investment>>

    @Query("SELECT * FROM investment_entries ORDER BY date DESC, id DESC")
    fun investmentEntries(): Flow<List<InvestmentEntry>>

    @Insert
    suspend fun insertInvestment(i: Investment): Long

    @Update
    suspend fun updateInvestment(i: Investment)

    @Insert
    suspend fun insertInvestmentEntry(e: InvestmentEntry): Long

    @Query("DELETE FROM investment_entries WHERE investmentId = :id")
    suspend fun deleteInvestmentEntries(id: Long)

    @Query("DELETE FROM investments WHERE id = :id")
    suspend fun deleteInvestment(id: Long)

    // ---------- Debts ----------
    @Query("SELECT * FROM debts ORDER BY settled, createdAt DESC")
    fun debts(): Flow<List<Debt>>

    @Query("SELECT * FROM debt_payments ORDER BY date DESC, id DESC")
    fun debtPayments(): Flow<List<DebtPayment>>

    @Insert
    suspend fun insertDebt(d: Debt): Long

    @Update
    suspend fun updateDebt(d: Debt)

    @Insert
    suspend fun insertDebtPayment(p: DebtPayment): Long

    @Query("DELETE FROM debt_payments WHERE debtId = :id")
    suspend fun deleteDebtPayments(id: Long)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebt(id: Long)

    // ---------- Recurring & subscriptions ----------
    @Query("SELECT * FROM recurring ORDER BY nextDate")
    fun recurring(): Flow<List<Recurring>>

    @Query("SELECT * FROM recurring WHERE active = 1")
    suspend fun activeRecurring(): List<Recurring>

    @Query("SELECT * FROM recurring WHERE id = :id")
    suspend fun getRecurring(id: Long): Recurring?

    @Insert
    suspend fun insertRecurring(r: Recurring): Long

    @Update
    suspend fun updateRecurring(r: Recurring)

    @Query("DELETE FROM recurring WHERE id = :id")
    suspend fun deleteRecurring(id: Long)

    // ---------- Budgets ----------
    @Query("SELECT * FROM budgets ORDER BY categoryName COLLATE NOCASE")
    fun budgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    suspend fun budgetList(): List<Budget>

    @Insert
    suspend fun insertBudget(b: Budget): Long

    @Update
    suspend fun updateBudget(b: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE kind = 'EXPENSE' AND currency = :currency " +
            "AND date >= :from AND date < :to " +
            "AND ((:bid IS NULL AND businessId IS NULL) OR businessId = :bid) " +
            "AND (:catId IS NULL OR categoryId = :catId)"
    )
    suspend fun spent(bid: Long?, catId: Long?, currency: String, from: Long, to: Long): Long

    // ---------- Bank alert suggestions ----------
    @Query("SELECT * FROM sms_suggestions WHERE status = 'PENDING' ORDER BY date DESC")
    fun pendingSuggestions(): Flow<List<SmsSuggestion>>

    /** Returns -1 when the same alert was already seen. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSuggestion(s: SmsSuggestion): Long

    @Query("SELECT * FROM sms_suggestions WHERE id = :id")
    suspend fun getSuggestion(id: Long): SmsSuggestion?

    @Query("UPDATE sms_suggestions SET status = :status WHERE id = :id")
    suspend fun setSuggestionStatus(id: Long, status: String)

    @Query("UPDATE sms_suggestions SET status = 'DISMISSED' WHERE status = 'PENDING'")
    suspend fun dismissAllSuggestions()

    // ---------- Backup: read everything ----------
    @Query("SELECT * FROM businesses") suspend fun allBusinesses(): List<Business>
    @Query("SELECT * FROM categories") suspend fun allCategories(): List<Category>
    @Query("SELECT * FROM transactions") suspend fun allTxns(): List<Txn>
    @Query("SELECT * FROM savings_goals") suspend fun allGoals(): List<Goal>
    @Query("SELECT * FROM goal_entries") suspend fun allGoalEntries(): List<GoalEntry>
    @Query("SELECT * FROM investments") suspend fun allInvestments(): List<Investment>
    @Query("SELECT * FROM investment_entries") suspend fun allInvestmentEntries(): List<InvestmentEntry>
    @Query("SELECT * FROM debts") suspend fun allDebts(): List<Debt>
    @Query("SELECT * FROM debt_payments") suspend fun allDebtPayments(): List<DebtPayment>
    @Query("SELECT * FROM recurring") suspend fun allRecurring(): List<Recurring>
    @Query("SELECT * FROM budgets") suspend fun allBudgets(): List<Budget>

    // ---------- Restore: wipe & write everything ----------
    @Query("DELETE FROM businesses") suspend fun clearBusinesses()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM transactions") suspend fun clearTxns()
    @Query("DELETE FROM savings_goals") suspend fun clearGoals()
    @Query("DELETE FROM goal_entries") suspend fun clearGoalEntries()
    @Query("DELETE FROM investments") suspend fun clearInvestments()
    @Query("DELETE FROM investment_entries") suspend fun clearInvestmentEntries()
    @Query("DELETE FROM debts") suspend fun clearDebts()
    @Query("DELETE FROM debt_payments") suspend fun clearDebtPayments()
    @Query("DELETE FROM recurring") suspend fun clearRecurring()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBusinesses(l: List<Business>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCategories(l: List<Category>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putTxns(l: List<Txn>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGoals(l: List<Goal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putGoalEntries(l: List<GoalEntry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putInvestments(l: List<Investment>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putInvestmentEntries(l: List<InvestmentEntry>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDebts(l: List<Debt>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDebtPayments(l: List<DebtPayment>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRecurring(l: List<Recurring>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBudgets(l: List<Budget>)
}
