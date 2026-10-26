package com.fintrack.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

data class Forecast(val spentSoFar: Double, val projected: Double, val basedOnMonths: Int)

/** A spend far above what the user normally pays in that category. */
data class Unusual(val txn: Txn, val typicalAmount: Double) {
    val times get() = txn.amount / typicalAmount
}

/**
 * Learns the user's normal spending from their history: typical monthly totals, how spending
 * usually builds up through a month (for the month-end forecast), and what counts as an
 * unusually large payment in each category. Pure statistics, computed on the phone.
 */
object Patterns {
    /** Spends that count towards totals, optionally limited to one category. */
    private fun spends(txns: List<Txn>, category: String?) = txns.filter {
        it.direction == Direction.DEBIT && it.category !in Categorizer.EXCLUDED_FROM_TOTALS &&
            (category == null || it.category == category)
    }

    fun monthlyTotals(history: List<Txn>, months: List<YearMonth>, category: String? = null): List<Pair<YearMonth, Double>> {
        val byMonth = spends(history, category).groupBy { YearMonth.from(it.date) }
        return months.map { m -> m to (byMonth[m]?.sumOf { it.amount } ?: 0.0) }
    }

    /** Months before [current] that have any transactions at all (so a quiet month counts as 0, a missing one doesn't). */
    private fun pastMonthsWithData(history: List<Txn>, current: YearMonth, lookback: Int): List<YearMonth> {
        val present = history.map { YearMonth.from(it.date) }.toSet()
        return (1..lookback).map { current.minusMonths(it.toLong()) }.filter { it in present }
    }

    /** Median monthly spend over past months; null with fewer than [minMonths] months of history. */
    fun typicalMonthly(history: List<Txn>, current: YearMonth, category: String? = null, lookback: Int = 6, minMonths: Int = 2): Double? {
        val months = pastMonthsWithData(history, current, lookback)
        if (months.size < minMonths) return null
        return median(monthlyTotals(history, months, category).map { it.second })
    }

    /**
     * Projects month-end spending. Uses how much of a month's spending has usually happened by
     * this day in past months (so rent on the 1st doesn't inflate the projection); falls back to
     * a straight-line pace after the first week. Null when there isn't enough to go on.
     */
    fun forecast(history: List<Txn>, current: YearMonth, today: LocalDate, category: String? = null): Forecast? {
        if (YearMonth.from(today) != current) return null
        val day = today.dayOfMonth
        val soFar = spends(history, category).filter { YearMonth.from(it.date) == current && it.date.dayOfMonth <= day }.sumOf { it.amount }

        val past = pastMonthsWithData(history, current, 6)
        val fractions = past.mapNotNull { m ->
            val monthSpends = spends(history, category).filter { YearMonth.from(it.date) == m }
            val total = monthSpends.sumOf { it.amount }
            if (total <= 0) return@mapNotNull null
            // Compare like for like when the past month is shorter than today's date.
            val cutoff = minOf(day, m.lengthOfMonth())
            monthSpends.filter { it.date.dayOfMonth <= cutoff }.sumOf { it.amount } / total
        }
        if (fractions.size >= 2) {
            val typicalFraction = median(fractions)
            if (typicalFraction >= 0.2) return Forecast(soFar, maxOf(soFar, soFar / typicalFraction), fractions.size)
            // Usually little is spent this early: expect the typical month, never less than what's spent.
            val typical = typicalMonthly(history, current, category) ?: return null
            return Forecast(soFar, maxOf(soFar, typical), fractions.size)
        }
        if (day < 7 || soFar <= 0) return null
        return Forecast(soFar, soFar / day * current.lengthOfMonth(), 0)
    }

    /**
     * Payments in [current] well above the user's usual amount for that category: at least
     * 2.5× the median and ₹500, and a robust outlier (median/MAD z-score ≥ 3.5) when amounts vary.
     * Needs at least [minPeers] earlier payments in the category.
     */
    fun unusual(history: List<Txn>, current: YearMonth, minPeers: Int = 5): List<Unusual> {
        val before = spends(history, null).filter { YearMonth.from(it.date) < current }.groupBy { it.category }
        return spends(history, null).filter { YearMonth.from(it.date) == current }.mapNotNull { t ->
            val peers = before[t.category]?.map { it.amount } ?: return@mapNotNull null
            if (peers.size < minPeers) return@mapNotNull null
            val med = median(peers)
            val mad = median(peers.map { abs(it - med) })
            val big = t.amount >= 500 && t.amount >= 2.5 * med
            val outlier = mad == 0.0 || (t.amount - med) / (1.4826 * mad) >= 3.5
            if (big && outlier) Unusual(t, med) else null
        }.sortedByDescending { it.times }
    }

    /** Running total per day of [month] (index 0 = day 1), up to [untilDay] when given. */
    fun cumulative(txns: List<Txn>, month: YearMonth, untilDay: Int? = null, category: String? = null): List<Double> {
        val perDay = spends(txns, category).filter { YearMonth.from(it.date) == month }
            .groupBy { it.date.dayOfMonth }.mapValues { (_, v) -> v.sumOf { it.amount } }
        var running = 0.0
        return (1..(untilDay ?: month.lengthOfMonth())).map { d -> running += perDay[d] ?: 0.0; running }
    }

    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val s = values.sorted()
        return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
    }
}
