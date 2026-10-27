package com.fintrack.core

import java.time.YearMonth
import kotlin.math.abs

/**
 * First run, statement first: the user imports a month's bank statement and FinTrack sorts as much
 * as the statement itself allows, so the user is only asked what it can't know, in the easiest form:
 *
 * - **Brands** in the name, narration or UPI ID (`flipkart.rzp@axisbank`, `swiggy@icici`) are sorted
 *   without asking.
 * - **Small payees** (under ₹500 in all) become one question: "daily small spends?".
 * - **People** (personal UPI IDs: a phone number or a name at a bank handle) are asked *who* they
 *   are (landlord, friend or family, your own account), which people remember, rather than what
 *   each payment was for.
 * - **Shops** (QR-code merchant UPI IDs) get shop categories as quick answers.
 * - **Same amount every month** is pointed out (rent, an EMI, fees or a bill).
 * - Biggest payees come first, and the user can stop once most of the money is sorted.
 */
object Onboarding {
    enum class Kind { BRAND, SHOP, PERSON, UNKNOWN }

    /** All payments to one payee (same UPI ID, or the same name however it's written). */
    data class Payee(val name: String, val txns: List<Txn>) {
        val total get() = txns.sumOf { it.amount }
        val upi: String? get() = txns.firstNotNullOfOrNull(PayeeMemory::upiId)

        /** The category a brand in the name, narration or UPI ID gives, if any. */
        val brandCategory: String? get() = txns.asSequence()
            .map { Categorizer.keywordCategory(it.merchant, Direction.DEBIT, it.raw) }
            .firstOrNull { it != "Other" && it != "Income" }

        val kind: Kind get() {
            if (brandCategory != null) return Kind.BRAND
            val id = upi ?: txns.firstNotNullOfOrNull { it.raw }?.let { raw -> UPI_ANY.find(raw)?.value }
            return when {
                id != null && SHOP_UPI.containsMatchIn(id) -> Kind.SHOP
                id != null && PERSON_UPI.containsMatchIn(id) -> Kind.PERSON
                txns.any { it.raw != null && P2A.containsMatchIn(it.raw) } -> Kind.PERSON
                txns.any { it.raw != null && P2M.containsMatchIn(it.raw) } -> Kind.SHOP
                else -> Kind.UNKNOWN
            }
        }

        /** Paid about the same amount (±1%) in two or more different months: rent, an EMI, fees, a bill. */
        val monthly: Boolean get() {
            val byMonth = txns.groupBy { YearMonth.from(it.date) }
            if (byMonth.size < 2) return false
            val amounts = byMonth.values.map { list -> list.maxOf { it.amount } }
            return amounts.all { abs(it - amounts.first()) <= amounts.first() * 0.01 }
        }

        /** FinTrack's current guess for this payee, if it has a real one. */
        val suggestion: String? get() = brandCategory ?: txns.map { it.category }
            .filter { it != "Other" && it != "Income" && it != "Transfers" && it != "Self Transfer" && it != Categorizer.SMALL }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }

    /**
     * What setup does with a statement: [auto] is sorted from brands alone, [small] is asked as one
     * question, [ask] is asked payee by payee, biggest first.
     */
    data class Plan(val auto: List<Payee>, val small: List<Payee>, val ask: List<Payee>) {
        val total get() = (auto + small + ask).sumOf { it.total }
    }

    fun plan(txns: List<Txn>): Plan {
        val payees = toSort(txns)
        val auto = payees.filter { it.kind == Kind.BRAND }
        val rest = payees - auto.toSet()
        val small = rest.filter { it.total < SMALL_PAYEE && !it.monthly }
        return Plan(auto, small, rest - small.toSet())
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
    /** Payees paid less than this in total are asked about together, not one by one. */
    const val SMALL_PAYEE = 500.0
    /** Once this share of the money is sorted, setup offers to stop. */
    const val ENOUGH = 0.9

    private val I = RegexOption.IGNORE_CASE
    private val UPI_ANY = Regex("""[a-z0-9][\w.\-]+@[a-z][a-z0-9\-]+""", I)
    /** Merchant QR codes and business handles: Paytm/BharatPe QR, PhonePe "Q…" merchant IDs, Google Pay for Business… */
    private val SHOP_UPI = Regex("""paytmqr|bharatpe|^q\d{5,}@|@okbiz|\.rzp@|razorpay|gpay-\d|@yespay|@fbpe|@pthdfc|@ptys|merchant|\bmer\b""", I)
    /** A phone number, or a name at a personal bank handle (okaxis, okhdfcbank, oksbi, okicici, ybl…). */
    private val PERSON_UPI = Regex("""^\d{10}(-\d)?@|@ok(axis|hdfcbank|icici|sbi)$|@(ybl|ibl|axl|apl|upi|paytm)$""", I)
    private val P2A = Regex("""\bp2a\b""", I)
    private val P2M = Regex("""\bp2m\b""", I)
}
