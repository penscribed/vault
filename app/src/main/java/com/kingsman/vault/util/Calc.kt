package com.kingsman.vault.util

import com.kingsman.vault.data.Budget
import com.kingsman.vault.data.Debt
import com.kingsman.vault.data.DebtPayment
import com.kingsman.vault.data.Frequency
import com.kingsman.vault.data.Goal
import com.kingsman.vault.data.GoalEntry
import com.kingsman.vault.data.Investment
import com.kingsman.vault.data.InvestmentEntry
import com.kingsman.vault.data.Kind
import com.kingsman.vault.data.Recurring
import com.kingsman.vault.data.Txn
import java.time.YearMonth

data class Totals(val income: Long = 0, val expense: Long = 0) {
    val net: Long get() = income - expense
}

object Calc {
    fun totals(txns: List<Txn>, currency: String): Totals {
        var i = 0L
        var e = 0L
        txns.filter { it.currency == currency }.forEach {
            if (it.kind == Kind.INCOME) i += it.amount else e += it.amount
        }
        return Totals(i, e)
    }

    fun totalsByCurrency(txns: List<Txn>): Map<String, Totals> =
        txns.groupBy { it.currency }.mapValues { (c, l) -> totals(l, c) }

    fun byCategory(txns: List<Txn>, kind: String, currency: String): List<Pair<String, Long>> =
        txns.filter { it.kind == kind && it.currency == currency }
            .groupBy { it.categoryName }
            .map { (k, v) -> k to v.sumOf { it.amount } }
            .sortedByDescending { it.second }

    fun monthly(txns: List<Txn>, currency: String, months: List<YearMonth>): List<Triple<YearMonth, Long, Long>> {
        val byMonth = txns.filter { it.currency == currency }
            .groupBy { YearMonth.from(Dates.toLocalDate(it.date)) }
        return months.map { ym ->
            val l = byMonth[ym].orEmpty()
            Triple(
                ym,
                l.filter { it.kind == Kind.INCOME }.sumOf { it.amount },
                l.filter { it.kind == Kind.EXPENSE }.sumOf { it.amount }
            )
        }
    }

    fun saved(goal: Goal, entries: List<GoalEntry>): Long =
        entries.filter { it.goalId == goal.id }.sumOf { it.amount }

    fun invested(inv: Investment, entries: List<InvestmentEntry>): Long =
        entries.filter { it.investmentId == inv.id }.sumOf { it.amount }

    fun paid(debt: Debt, payments: List<DebtPayment>): Long =
        payments.filter { it.debtId == debt.id }.sumOf { it.amount }

    fun remaining(debt: Debt, payments: List<DebtPayment>): Long =
        if (debt.settled) 0 else (debt.amount - paid(debt, payments)).coerceAtLeast(0)

    /** Cost per month of a recurring item (for subscription totals). */
    fun monthlyEquivalent(r: Recurring): Long = when (r.frequency) {
        Frequency.WEEKLY -> r.amount * 52 / 12
        Frequency.QUARTERLY -> r.amount / 3
        Frequency.YEARLY -> r.amount / 12
        else -> r.amount
    }

    /** This month's spending that counts against a budget ([monthTxns] = current scope, current month). */
    fun budgetSpent(b: Budget, monthTxns: List<Txn>): Long =
        monthTxns.filter {
            it.kind == Kind.EXPENSE && it.currency == b.currency &&
                (b.categoryId == null || it.categoryId == b.categoryId)
        }.sumOf { it.amount }

    fun sumByCurrency(items: List<Pair<String, Long>>): Map<String, Long> =
        items.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
}
