package com.fintrack.core

import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Remembers the categories the user gave to payments (e.g. ₹3,200 to a person → Rent) and applies
 * them to other payments that are clearly the same thing:
 *
 * 1. **Same payee**: the same UPI ID, or the same name however it's written ("Manoop - V U" in a
 *    statement, "Manoop V U" in an SMS).
 * 2. **Same monthly payment**: no name match, but the same amount (within 1%) around the same day of
 *    a later month as a payment the user categorized; e.g. rent paid from a different app. Only for
 *    ₹500 and up, where an equal amount is unlikely to be a coincidence.
 *
 * Every match says why, and the user's own choices always win.
 */
class PayeeMemory(history: List<Txn>) {
    data class Match(val category: String, val reason: String)

    private val taught = history
        .filter { it.direction == Direction.DEBIT && it.category !in NOT_TAUGHT &&
            (it.categorySource == CategorySource.USER || it.categorySource == CategorySource.RULE) }
        .sortedByDescending { it.date }
    private val byUpi = taught.mapNotNull { t -> upiId(t)?.let { it to t } }.distinctBy { it.first }.toMap()
    private val byName = taught.distinctBy { FlowAnalyzer.norm(it.merchant) }.associateBy { FlowAnalyzer.norm(it.merchant) }

    fun match(t: Txn): Match? {
        if (t.direction != Direction.DEBIT) return null
        upiId(t)?.let { id -> byUpi[id]?.let { return Match(it.category, "Same UPI ID as ${it.merchant} (${it.category})") } }
        byName[FlowAnalyzer.norm(t.merchant)]?.takeIf { t.merchant != "Unknown" }?.let {
            return Match(it.category, "You put ${it.merchant} in ${it.category}")
        }
        if (t.amount < MIN_PATTERN_AMOUNT) return null
        val same = taught.firstOrNull { p ->
            p.id != t.id && abs(p.amount - t.amount) <= t.amount * 0.01 && p.date < t.date &&
                ChronoUnit.DAYS.between(p.date, t.date) >= 20 && dayGap(p, t) <= 6
        } ?: return null
        return Match(same.category, "Same amount as your ${same.category} payment on ${same.date.dayOfMonth} ${same.date.month.name.lowercase().replaceFirstChar(Char::uppercase).take(3)}")
    }

    companion object {
        const val MIN_PATTERN_AMOUNT = 500.0
        private val NOT_TAUGHT = setOf("Other", "Income", "Transfers", "Self Transfer", Categorizer.SMALL)
        private val UPI = Regex("""\b([a-z0-9][\w.\-]{1,}@[a-z][a-z0-9\-]{1,})\b""", RegexOption.IGNORE_CASE)
        /** Payment gateways: many different businesses share these IDs, so they say nothing about the payee. */
        private val GATEWAY = Regex("""razorpay|billdesk|payu|cashfree|ccavenue|juspay|easebuzz|instamojo""", RegexOption.IGNORE_CASE)

        fun upiId(t: Txn): String? = t.raw?.let { raw -> UPI.findAll(raw).map { it.groupValues[1].lowercase() }.firstOrNull { !GATEWAY.containsMatchIn(it) } }

        /** Days between the two dates' day-of-month, wrapping round the month end (30th vs 2nd = 2). */
        private fun dayGap(a: Txn, b: Txn): Int {
            val d = abs(a.date.dayOfMonth - b.date.dayOfMonth)
            return minOf(d, 31 - d)
        }

        /** Categories that mean "I spent this", so a payment to a person in one of them is spending. */
        fun isSpending(category: String) = category !in NOT_TAUGHT && category != "Investments"
    }
}
