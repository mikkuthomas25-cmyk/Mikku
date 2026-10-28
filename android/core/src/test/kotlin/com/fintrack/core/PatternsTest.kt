package com.fintrack.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PatternsTest {
    private var id = 1L
    private fun spend(date: LocalDate, amount: Double, category: String = "Food & Dining", merchant: String = "Swiggy") =
        Txn(id++, date, amount, Direction.DEBIT, merchant, category)

    /** Three past months: rent ₹15,000 on the 1st, ₹200 food every day. */
    private fun history(months: List<YearMonth>) = months.flatMap { m ->
        listOf(spend(m.atDay(1), 15000.0, "Rent", "Landlord")) + (1..m.lengthOfMonth()).map { spend(m.atDay(it), 200.0) }
    }

    private val oct = YearMonth.of(2026, 10)
    private val past = listOf(YearMonth.of(2026, 7), YearMonth.of(2026, 8), YearMonth.of(2026, 9))

    @Test fun typicalMonthlyIsTheMedianOfPastMonths() {
        val h = history(past)
        assertEquals(200.0 * 31, Patterns.typicalMonthly(h, oct, "Food & Dining"))  // Jul 31, Aug 31, Sep 30 → median 31 days
        assertNull(Patterns.typicalMonthly(history(past.takeLast(1)), oct, "Food & Dining"))
    }

    @Test fun forecastKnowsRentComesFirst() {
        // On the 10th: rent paid + 10 days of food. A straight line would wildly overshoot.
        val h = history(past) + spend(oct.atDay(1), 15000.0, "Rent", "Landlord") + (1..10).map { spend(oct.atDay(it), 200.0) }
        val f = assertNotNull(Patterns.forecast(h, oct, oct.atDay(10)))
        val expected = 15000.0 + 200.0 * 31
        assertTrue(f.projected in expected * 0.95..expected * 1.05, "projected ${f.projected}, expected ≈$expected")
        assertEquals(17000.0, f.spentSoFar)
        assertEquals(3, f.basedOnMonths)
    }

    @Test fun forecastFallsBackToPaceAfterAWeekWithNoHistory() {
        val h = (1..10).map { spend(oct.atDay(it), 300.0) }
        assertEquals(300.0 * 31, Patterns.forecast(h, oct, oct.atDay(10))!!.projected, 0.01)
        assertNull(Patterns.forecast(h.take(3), oct, oct.atDay(3)))
    }

    @Test fun forecastOnlyForTheCurrentMonth() {
        assertNull(Patterns.forecast(history(past), YearMonth.of(2026, 9), oct.atDay(10)))
    }

    @Test fun flagsUnusuallyLargeSpends() {
        val h = history(past) + spend(oct.atDay(5), 200.0) + spend(oct.atDay(6), 2400.0, merchant = "Fancy Dinner")
        val u = Patterns.unusual(h, oct)
        assertEquals(1, u.size)
        assertEquals("Fancy Dinner", u.single().txn.merchant)
        assertEquals(12.0, u.single().times, 0.01)
    }

    @Test fun doesNotFlagNormalVariationOrThinHistory() {
        val varied = (1..20).map { spend(YearMonth.of(2026, 9).atDay(it), 100.0 + it * 40) }
        assertTrue(Patterns.unusual(varied + spend(oct.atDay(3), 650.0), oct).isEmpty())
        val thin = (1..3).map { spend(YearMonth.of(2026, 9).atDay(it), 100.0) }
        assertTrue(Patterns.unusual(thin + spend(oct.atDay(3), 5000.0), oct).isEmpty())
    }

    @Test fun cumulativeRunsDayByDay() {
        val h = listOf(spend(oct.atDay(1), 100.0), spend(oct.atDay(3), 50.0), spend(oct.atDay(3), 25.0))
        assertEquals(listOf(100.0, 100.0, 175.0, 175.0), Patterns.cumulative(h, oct, untilDay = 4))
    }

    @Test fun monthsWithBarelyAnyDataDontSetTheUsualMonth() {
        val oct = YearMonth.of(2026, 10)
        var n = 1000L
        fun spend(m: YearMonth, day: Int, amount: Double) =
            Txn(n++, m.atDay(day), amount, Direction.DEBIT, "Shop", "Other", null, "sms", null, flow = Flow.SPEND)
        // Two SMS a month before statements were imported; then full months of 30 payments.
        val sparse = (4L..6L).flatMap { k -> listOf(spend(oct.minusMonths(k), 3, 500.0), spend(oct.minusMonths(k), 9, 600.0)) }
        val full = (1L..3L).flatMap { k -> (1..30).map { d -> spend(oct.minusMonths(k), d, 1000.0) } }
        assertEquals(30000.0, Patterns.typicalMonthly(sparse + full, oct))
    }
}
