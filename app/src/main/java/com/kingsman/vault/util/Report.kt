package com.kingsman.vault.util

import com.kingsman.vault.data.Debt
import com.kingsman.vault.data.DebtPayment
import com.kingsman.vault.data.Direction
import com.kingsman.vault.data.Goal
import com.kingsman.vault.data.GoalEntry
import com.kingsman.vault.data.Investment
import com.kingsman.vault.data.InvestmentEntry
import com.kingsman.vault.data.Kind
import com.kingsman.vault.data.Txn
import java.time.YearMonth

data class GoalRow(val name: String, val saved: Long, val target: Long, val inPeriod: Long)
data class InvRow(val name: String, val type: String, val invested: Long, val current: Long)
data class DebtRow(val person: String, val direction: String, val remaining: Long, val due: Long?)

data class ReportData(
    val scopeName: String,
    val currency: String,
    val periodLabel: String,
    val from: Long,
    val to: Long,
    val totals: Totals,
    val expenseByCat: List<Pair<String, Long>>,
    val incomeByCat: List<Pair<String, Long>>,
    val monthly: List<Triple<YearMonth, Long, Long>>,
    val goals: List<GoalRow>,
    val investments: List<InvRow>,
    val debts: List<DebtRow>,
    val owedToMe: Long,
    val iOwe: Long,
    val owedByBusinesses: Long,
    val txns: List<Txn>
) {
    val savedTotal get() = goals.sumOf { it.saved }
    val savedInPeriod get() = goals.sumOf { it.inPeriod }
    val investedTotal get() = investments.sumOf { it.invested }
    val investmentValue get() = investments.sumOf { it.current }
    val savingsRate: Int
        get() = if (totals.income <= 0) 0 else ((totals.net * 100) / totals.income).toInt()
}

object ReportBuilder {
    fun build(
        scopeName: String,
        currency: String,
        periodLabel: String,
        from: Long,
        to: Long,
        txns: List<Txn>,
        goals: List<Goal>,
        goalEntries: List<GoalEntry>,
        investments: List<Investment>,
        investmentEntries: List<InvestmentEntry>,
        debts: List<Debt>,
        debtPayments: List<DebtPayment>,
        owedByBusinesses: List<Txn>
    ): ReportData {
        val inCur = txns.filter { it.currency == currency }
        val startYm = YearMonth.from(Dates.toLocalDate(from))
        val endYm = YearMonth.from(Dates.toLocalDate(to - 1))
        val months = generateSequence(startYm) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(endYm) }
            .take(36)
            .toList()

        val goalRows = goals.filter { it.currency == currency }.map { g ->
            GoalRow(
                g.name,
                Calc.saved(g, goalEntries),
                g.target,
                goalEntries.filter { it.goalId == g.id && it.date in from until to }.sumOf { it.amount }
            )
        }
        val invRows = investments.filter { it.currency == currency }.map {
            InvRow(it.name, it.type, Calc.invested(it, investmentEntries), it.currentValue)
        }
        val openDebts = debts.filter { it.currency == currency && !it.settled }
        val debtRows = openDebts.map {
            DebtRow(it.person, it.direction, Calc.remaining(it, debtPayments), it.dueDate)
        }.filter { it.remaining > 0 }

        return ReportData(
            scopeName = scopeName,
            currency = currency,
            periodLabel = periodLabel,
            from = from,
            to = to,
            totals = Calc.totals(inCur, currency),
            expenseByCat = Calc.byCategory(inCur, Kind.EXPENSE, currency),
            incomeByCat = Calc.byCategory(inCur, Kind.INCOME, currency),
            monthly = if (months.size > 1) Calc.monthly(inCur, currency, months) else emptyList(),
            goals = goalRows,
            investments = invRows,
            debts = debtRows,
            owedToMe = debtRows.filter { it.direction == Direction.OWED_TO_ME }.sumOf { it.remaining },
            iOwe = debtRows.filter { it.direction == Direction.I_OWE }.sumOf { it.remaining },
            owedByBusinesses = owedByBusinesses.filter { it.currency == currency }.sumOf { it.amount },
            txns = inCur.sortedByDescending { it.date }
        )
    }
}
