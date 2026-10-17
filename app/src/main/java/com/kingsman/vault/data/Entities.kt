package com.kingsman.vault.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * All money is stored as Long "minor units" (amount x 100) to avoid rounding errors.
 * businessId == null means the record belongs to Personal mode.
 */

object Kind {
    const val INCOME = "INCOME"
    const val EXPENSE = "EXPENSE"
}

object Scope {
    const val PERSONAL = "PERSONAL"
    const val BUSINESS = "BUSINESS"
}

object Direction {
    const val OWED_TO_ME = "OWED_TO_ME"
    const val I_OWE = "I_OWE"
}

object Frequency {
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val QUARTERLY = "QUARTERLY"
    const val YEARLY = "YEARLY"
    val all = listOf(WEEKLY, MONTHLY, QUARTERLY, YEARLY)
    fun label(f: String) = when (f) {
        WEEKLY -> "Weekly"
        MONTHLY -> "Monthly"
        QUARTERLY -> "Quarterly"
        YEARLY -> "Yearly"
        else -> f
    }
}

@Entity(tableName = "businesses")
data class Business(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val archived: Boolean = false
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,
    val scope: String,
    val archived: Boolean = false
)

@Entity(
    tableName = "transactions",
    indices = [Index("businessId"), Index("date"), Index("categoryId")]
)
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val amount: Long,
    val currency: String,
    val categoryId: Long?,
    val categoryName: String,
    val businessId: Long?,
    val date: Long,
    val note: String = "",
    val tag: String = "",
    /** Business expense paid out of the owner's personal money. */
    val paidPersonally: Boolean = false,
    val reimbursed: Boolean = false,
    val recurringId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** Encrypted receipt photo file name (in app storage), if any. */
    val receiptPath: String? = null
)

@Entity(tableName = "savings_goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long,
    val currency: String,
    val deadline: Long? = null,
    val businessId: Long? = null,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** Positive = money put into the goal, negative = withdrawn. */
@Entity(tableName = "goal_entries", indices = [Index("goalId")])
data class GoalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Long,
    val date: Long,
    val note: String = ""
)

@Entity(tableName = "investments")
data class Investment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val currency: String,
    val currentValue: Long,
    val valueUpdatedAt: Long = System.currentTimeMillis(),
    val businessId: Long? = null,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** Positive = money put in, negative = money taken out. */
@Entity(tableName = "investment_entries", indices = [Index("investmentId")])
data class InvestmentEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val investmentId: Long,
    val amount: Long,
    val date: Long,
    val note: String = ""
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val direction: String,
    val amount: Long,
    val currency: String,
    val dueDate: Long? = null,
    val note: String = "",
    val businessId: Long? = null,
    val settled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "debt_payments", indices = [Index("debtId")])
data class DebtPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val date: Long,
    val note: String = ""
)

/**
 * Recurring entries and subscriptions.
 * variable = true means the amount changes each time (e.g. commission):
 * the app reminds you instead of logging it automatically.
 */
@Entity(tableName = "recurring")
data class Recurring(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val kind: String,
    val amount: Long,
    val variable: Boolean,
    val currency: String,
    val categoryId: Long?,
    val categoryName: String,
    val businessId: Long?,
    val frequency: String,
    val nextDate: Long,
    val isSubscription: Boolean,
    val active: Boolean = true,
    val note: String = ""
)

/**
 * Monthly spending limit. categoryId == null means "all spending".
 * alertedMonth/alertedLevel remember which alert was already sent this month
 * (1 = reached the alert %, 2 = over budget) so you aren't notified twice.
 */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long?,
    val categoryName: String,
    val businessId: Long?,
    val currency: String,
    val amount: Long,
    val alertPercent: Int = 80,
    val alertedMonth: String = "",
    val alertedLevel: Int = 0
)

object SuggestionStatus {
    const val PENDING = "PENDING"
    const val ADDED = "ADDED"
    const val DISMISSED = "DISMISSED"
}

/** A bank alert (SMS or pasted text) that Vault turned into a suggested entry. */
@Entity(tableName = "sms_suggestions", indices = [Index(value = ["hash"], unique = true)])
data class SmsSuggestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hash: String,
    val sender: String,
    val body: String,
    val kind: String,
    val amount: Long,
    val currency: String,
    val description: String,
    val date: Long,
    val status: String = SuggestionStatus.PENDING
)

data class CatCount(val categoryId: Long?, val c: Int)
