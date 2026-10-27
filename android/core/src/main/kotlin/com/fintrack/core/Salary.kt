package com.fintrack.core

import java.time.YearMonth
import kotlin.math.abs

/**
 * Salary paid at the end of a month (e.g. on the 30th) is what you live on next month. Counting it
 * in the calendar month it lands makes one month look rich and the next one empty, so a salary
 * credited in the last week counts toward the following month.
 */
object Salary {
    const val SHIFT_FROM_DAY = 24
    private const val MIN_AMOUNT = 10_000.0
    private val WORDED = Regex("""\b(salary|sal|payroll)\b""", RegexOption.IGNORE_CASE)

    /**
     * Credits that look like salary: worded as salary, or a big income credit (₹10,000+) that
     * recurs at about the same amount (±15%) in another month.
     */
    fun ids(txns: List<Txn>): Set<Long> {
        val big = txns.filter { it.direction == Direction.CREDIT && it.effectiveFlow == Flow.INCOME && it.amount >= MIN_AMOUNT }
        return big.filter { t ->
            WORDED.containsMatchIn(t.raw ?: "") || WORDED.containsMatchIn(t.merchant) ||
                big.any { o -> YearMonth.from(o.date) != YearMonth.from(t.date) && abs(o.amount - t.amount) <= 0.15 * t.amount }
        }.map { it.id }.toSet()
    }

    /** The month a transaction counts toward: the next one for late-month salary, else its own. */
    fun monthOf(t: Txn, salaryIds: Set<Long>): YearMonth {
        val m = YearMonth.from(t.date)
        return if (t.id in salaryIds && t.date.dayOfMonth >= SHIFT_FROM_DAY) m.plusMonths(1) else m
    }
}
