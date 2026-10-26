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
)

data class Amount(val label: String, val amount: Double, val count: Int = 0)

data class MonthSummary(
    val month: YearMonth,
    val spent: Double,
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
) {
    val net get() = received - spent

    /** Aggregates only: this is all the AI insights feature ever sends. */
    fun toPromptText(): String = buildString {
        appendLine("Month: $month")
        appendLine("Total spent: ₹%.0f (previous month: ₹%.0f)".format(spent, prevMonthSpent))
        appendLine("Total received: ₹%.0f".format(received))
        appendLine("Transactions: $txnCount; payments of ₹200 or less: $smallPaymentsCount totalling ₹%.0f"
            .format(smallPaymentsTotal))
        appendLine("Average spend per day so far: ₹%.0f".format(avgPerDay))
        appendLine("By category:")
        byCategory.forEach { appendLine("- ${it.label}: ₹%.0f".format(it.amount)) }
        appendLine("Top merchants:")
        topMerchants.forEach { appendLine("- ${it.label}: ₹%.0f across ${it.count} payments".format(it.amount)) }
    }
}

object Summaries {
    const val SMALL_PAYMENT = 200.0

    fun build(month: YearMonth, monthTxns: List<Txn>, prevMonthTxns: List<Txn>, today: LocalDate = LocalDate.now()): MonthSummary {
        val counted = monthTxns.filter { it.category !in Categorizer.EXCLUDED_FROM_TOTALS }
        val debits = counted.filter { it.direction == Direction.DEBIT }
        val spent = debits.sumOf { it.amount }

        val perDay = debits.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.amount } }
        val byDay = (1..month.lengthOfMonth()).map { d -> month.atDay(d).let { it to (perDay[it] ?: 0.0) } }

        val daysElapsed = when {
            today < month.atDay(1) -> 0
            YearMonth.from(today) == month -> today.dayOfMonth
            else -> month.lengthOfMonth()
        }
        val small = debits.filter { it.amount <= SMALL_PAYMENT }

        return MonthSummary(
            month = month,
            spent = spent,
            received = counted.filter { it.direction == Direction.CREDIT }.sumOf { it.amount },
            todaySpent = perDay[today] ?: 0.0,
            avgPerDay = if (daysElapsed > 0) spent / daysElapsed else 0.0,
            prevMonthSpent = prevMonthTxns
                .filter { it.direction == Direction.DEBIT && it.category !in Categorizer.EXCLUDED_FROM_TOTALS }
                .sumOf { it.amount },
            txnCount = counted.size,
            smallPaymentsCount = small.size,
            smallPaymentsTotal = small.sumOf { it.amount },
            byDay = byDay,
            byCategory = debits.groupBy { it.category }
                .map { (c, v) -> Amount(c, v.sumOf { it.amount }, v.size) }
                .sortedByDescending { it.amount },
            topMerchants = debits.groupBy { it.merchant }
                .map { (m, v) -> Amount(m, v.sumOf { it.amount }, v.size) }
                .sortedByDescending { it.amount }
                .take(10),
        )
    }
}
