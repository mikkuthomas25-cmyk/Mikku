package com.fintrack.core

import java.time.LocalDate
import java.time.YearMonth

data class Txn(
    val id: Long,
    val date: LocalDate,
    val amount: Double,
    val direction: Direction,
    val merchant: String,
    val category: String,
    val account: String? = null,
    val source: String = "sms",
    val raw: String? = null,
    val categorySource: String = CategorySource.KEYWORD,
    val ref: String? = null,
    /** Set by [FlowAnalyzer]; null means not analysed yet ([Flow.basic] applies). */
    val flow: Flow? = null,
) {
    val effectiveFlow: Flow get() = flow ?: Flow.basic(this)

    /** What this transaction does to "spent": + for spending, − for a refund, 0 otherwise. */
    val spendValue: Double get() = when (effectiveFlow) {
        Flow.SPEND -> amount
        Flow.REFUND -> -amount
        else -> 0.0
    }
}

data class Amount(val label: String, val amount: Double, val count: Int = 0)

data class MonthSummary(
    val month: YearMonth,
    /** Spending net of refunds. */
    val spent: Double,
    /** Money in: income plus money received from people. */
    val received: Double,
    val todaySpent: Double,
    val avgPerDay: Double,
    val prevMonthSpent: Double,
    val txnCount: Int,
    val smallPaymentsCount: Int,
    val smallPaymentsTotal: Double,
    val byDay: List<Pair<LocalDate, Double>>,
    val byCategory: List<Amount>,
    val topMerchants: List<Amount>,
    /** The money-flow statement: each line is the total for that [Flow] this month. */
    val byFlow: Map<Flow, Double> = emptyMap(),
) {
    /** Left over = money in − spent − invested − sent to people. Own transfers and duplicates don't count. */
    val net get() = received - spent - (byFlow[Flow.INVESTED] ?: 0.0) - (byFlow[Flow.TO_PEOPLE] ?: 0.0)
}

object Summaries {
    const val SMALL_PAYMENT = 200.0

    fun build(month: YearMonth, monthTxns: List<Txn>, prevMonthTxns: List<Txn>, today: LocalDate = LocalDate.now()): MonthSummary {
        val spends = monthTxns.filter { it.effectiveFlow == Flow.SPEND }
        val spent = monthTxns.sumOf { it.spendValue }.coerceAtLeast(0.0)

        val perDay = monthTxns.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.spendValue }.coerceAtLeast(0.0) }
        val byDay = (1..month.lengthOfMonth()).map { d -> month.atDay(d).let { it to (perDay[it] ?: 0.0) } }

        val daysElapsed = when {
            today < month.atDay(1) -> 0
            YearMonth.from(today) == month -> today.dayOfMonth
            else -> month.lengthOfMonth()
        }
        val small = spends.filter { it.amount <= SMALL_PAYMENT }
        val byFlow = monthTxns.groupBy { it.effectiveFlow }.mapValues { (_, v) -> v.sumOf { it.amount } }

        return MonthSummary(
            month = month,
            spent = spent,
            received = monthTxns.filter { it.effectiveFlow.countsAsMoneyIn }.sumOf { it.amount },
            todaySpent = perDay[today] ?: 0.0,
            avgPerDay = if (daysElapsed > 0) spent / daysElapsed else 0.0,
            prevMonthSpent = prevMonthTxns.sumOf { it.spendValue }.coerceAtLeast(0.0),
            txnCount = monthTxns.count { it.effectiveFlow.counted },
            smallPaymentsCount = small.size,
            smallPaymentsTotal = small.sumOf { it.amount },
            byDay = byDay,
            // Refunds reduce the category they came back to.
            byCategory = monthTxns.filter { it.spendValue != 0.0 }.groupBy { it.category }
                .map { (c, v) -> Amount(c, v.sumOf { it.spendValue }, v.count { it.effectiveFlow == Flow.SPEND }) }
                .filter { it.amount > 0.005 }
                .sortedByDescending { it.amount },
            topMerchants = spends.groupBy { it.merchant }
                .map { (m, v) -> Amount(m, v.sumOf { it.amount }, v.size) }
                .sortedByDescending { it.amount }
                .take(10),
            byFlow = byFlow,
        )
    }
}
