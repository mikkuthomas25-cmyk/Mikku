package com.fintrack.core

import java.time.YearMonth

/**
 * First run, statement first: the user imports a month's bank statement and sorts its spending
 * payee by payee, so FinTrack starts from their own categories instead of guesses.
 */
object Onboarding {
    /** All payments to one payee (same UPI ID, or the same name however it's written). */
    data class Payee(val name: String, val txns: List<Txn>) {
        val total get() = txns.sumOf { it.amount }
        /** FinTrack's current guess for this payee, if it has a real one. */
        val suggestion: String? get() = txns.map { it.category }
            .filter { it != "Other" && it != "Income" && it != "Transfers" && it != "Self Transfer" }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }

    /** Statement spending the user hasn't sorted yet, one entry per payee, biggest first. */
    fun toSort(txns: List<Txn>): List<Payee> = txns
        .filter {
            it.source == "statement" && it.direction == Direction.DEBIT &&
                it.categorySource != CategorySource.USER && it.categorySource != CategorySource.RULE
        }
        .groupBy { PayeeMemory.upiId(it) ?: FlowAnalyzer.norm(it.merchant) }
        .map { (_, list) -> Payee(list.groupingBy { it.merchant }.eachCount().maxByOrNull { it.value }!!.key, list.sortedBy { it.date }) }
        .sortedByDescending { it.total }

    /** Months covered by imported statements. One month is a start; two or more teach the regular payments. */
    fun statementMonths(txns: List<Txn>): Set<YearMonth> =
        txns.filter { it.source == "statement" }.map { YearMonth.from(it.date) }.toSet()

    const val RECOMMENDED_MONTHS = 2
}
