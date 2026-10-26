package com.fintrack.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SmsParserTest {
    private val samples = javaClass.getResource("/sample_sms.txt")!!.readText().split("\n\n").map { it.trim() }

    private data class Expected(val amount: Double, val dir: Direction, val date: String, val merchant: String, val account: String?)

    private val expected = listOf(
        Expected(250.0, Direction.DEBIT, "2026-10-12", "Swiggy", "1234"),
        Expected(120.0, Direction.DEBIT, "2026-10-13", "Zomato", "5678"),
        Expected(2499.0, Direction.DEBIT, "2026-10-13", "Amazon Pay", "9876"),
        Expected(50000.0, Direction.CREDIT, "2026-10-01", "Acme Technologies", "1234"),
        Expected(1500.0, Direction.DEBIT, "2026-10-05", "ATM Withdrawal", "1234"),
        Expected(640.0, Direction.DEBIT, "2026-10-14", "Uber", "5678"),
        Expected(500.0, Direction.CREDIT, "2026-10-15", "Rahul K", "5678"),
        Expected(15000.0, Direction.DEBIT, "2026-10-03", "Landlord Sharma", "1234"),
        Expected(899.0, Direction.DEBIT, "2026-10-10", "Netflix", "4321"),
        Expected(1250.5, Direction.DEBIT, "2026-10-16", "Bigbasket", "1234"),
    )

    @Test fun parsesTransactions() {
        expected.forEachIndexed { i, e ->
            val t = assertNotNull(SmsParser.parse(samples[i]), "sample $i")
            assertEquals(e, Expected(t.amount, t.direction, t.date.toString(), t.merchant, t.account), "sample $i")
        }
    }

    @Test fun ignoresOtpsPromosRemindersAndRequests() {
        samples.drop(expected.size).forEach { assertNull(SmsParser.parse(it), it) }
    }

    @Test fun fallsBackToReceivedDate() {
        val t = SmsParser.parse("Rs 99 debited from a/c XX1111 to VPA chai@paytm", LocalDate.of(2026, 9, 3))!!
        assertEquals(LocalDate.of(2026, 9, 3), t.date)
        assertEquals("Chai", t.merchant)
    }

    @Test fun hashIgnoresWhitespace() {
        assertEquals(SmsParser.hash(samples[0]), SmsParser.hash("  " + samples[0].replace(" ", "  ") + "\n"))
    }

    @Test fun keywordCategories() {
        mapOf(
            "Swiggy" to "Food & Dining", "Bigbasket" to "Groceries", "Uber" to "Transport",
            "Netflix" to "Entertainment", "ATM Withdrawal" to "Cash Withdrawal", "Landlord Sharma" to "Rent",
            "ICICI Credit Card XX9876 payment" to "Self Transfer", "Some Random Shop" to "Other",
        ).forEach { (m, c) -> assertEquals(c, Categorizer.keywordCategory(m, Direction.DEBIT), m) }
        assertEquals("Income", Categorizer.keywordCategory("Acme Technologies", Direction.CREDIT))
        assertEquals("Rent", Categorizer.categorize("Thirdwave", Direction.DEBIT, mapOf("thirdwave" to "Rent")))
    }

    @Test fun summaryExcludesSelfTransfersAndFillsEveryDay() {
        val oct = YearMonth.of(2026, 10)
        val txns = expected.mapIndexed { i, e ->
            Txn(i.toLong(), LocalDate.parse(e.date), e.amount, e.dir, e.merchant,
                Categorizer.keywordCategory(e.merchant, e.dir))
        } + Txn(99, LocalDate.of(2026, 10, 20), 2000.0, Direction.DEBIT, "Card bill", "Self Transfer")
        val s = Summaries.build(oct, txns, emptyList(), today = LocalDate.of(2026, 10, 26))
        assertEquals(250 + 120 + 2499 + 1500 + 640 + 15000 + 899 + 1250.5, s.spent)
        assertEquals(50500.0, s.received)
        assertEquals(31, s.byDay.size)
        assertEquals(2619.0, s.byDay.first { it.first.dayOfMonth == 13 }.second)
        assertEquals(Amount("Rent", 15000.0, 1), s.byCategory.first())
        assertEquals(s.spent / 26, s.avgPerDay)
    }
}
