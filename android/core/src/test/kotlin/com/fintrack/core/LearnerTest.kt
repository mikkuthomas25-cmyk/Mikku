package com.fintrack.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LearnerTest {
    private var nextId = 1L
    private fun txn(
        merchant: String, amount: Double, category: String = "Other", source: String = CategorySource.KEYWORD,
        dir: Direction = Direction.DEBIT, day: Int = 12, raw: String? = null,
    ) = Txn(nextId++, LocalDate.of(2026, 10, day), amount, dir, merchant, category, raw = raw, categorySource = source)

    @Test fun knowsBrandsFromTheBuiltInListOnDayOne() {
        val learner = CategoryLearner(emptyList())
        assertEquals("Food & Dining", learner.suggest(txn("Swiggy Bangalore", 320.0))?.category)
        assertEquals("Groceries", learner.suggest(txn("Instamart Order", 540.0))?.category)
    }

    @Test fun learnsFromACorrectionAndGeneralisesToSimilarNames() {
        val history = listOf(
            txn("Third Wave Coffee Indiranagar", 280.0, "Food & Dining", CategorySource.USER),
            txn("Cult Fitness Membership", 1999.0, "Health", CategorySource.USER),
        )
        val learner = CategoryLearner(history)
        assertEquals("Food & Dining", learner.suggest(txn("THIRD WAVE COFFEE HSR", 310.0))?.category)
        assertEquals("Health", learner.suggest(txn("Cult Fitness Renewal", 1999.0))?.category)
    }

    @Test fun learnsThatPersonalUpiPaymentsAreTransfers() {
        val history = (1..4).map {
            txn("Friend $it", 500.0 * it, "Transfers", CategorySource.USER, raw = "Rs ${500 * it} debited to VPA friend$it@okaxis UPI")
        }
        val learner = CategoryLearner(history)
        val s = learner.suggest(txn("Rahul Menon", 750.0, raw = "Rs 750 debited to VPA rahul.menon@oksbi UPI"))
        assertEquals("Transfers", s?.category)
    }

    @Test fun staysQuietAboutCompletelyUnfamiliarMerchants() {
        val learner = CategoryLearner(listOf(txn("Swiggy", 300.0, "Food & Dining", CategorySource.USER)))
        assertNull(learner.suggest(txn("Zxqv Pvt", 845.0)))
    }

    @Test fun neverTrainsOnItsOwnGuessesOrOnFallbacks() {
        val guesses = (1..20).map { txn("Mystery Shop $it", 100.0, "Travel", CategorySource.LEARNED) }
        val fallbacks = (1..20).map { txn("Unknown Payee $it", 100.0, "Other") }
        val learner = CategoryLearner(guesses + fallbacks)
        assertNull(learner.suggest(txn("Mystery Shop 99", 100.0)))
        assertTrue(CategoryLearner.isLearnable(guesses.first()) && CategoryLearner.isLearnable(fallbacks.first()))
    }

    @Test fun neverCallsASpendIncome() {
        val learner = CategoryLearner(listOf(txn("Acme Payroll", 50000.0, "Income", CategorySource.USER, Direction.CREDIT)))
        assertNull(learner.suggest(txn("Acme Payroll", 500.0)))
    }

    @Test fun userCorrectionsOutweighTheBuiltInList() {
        // The user files "Amazon" payments under Bills (e.g. Amazon Pay bill payments) several times.
        val history = (1..3).map { txn("Amazon Pay Bill", 899.0, "Bills & Utilities", CategorySource.USER) }
        assertEquals("Bills & Utilities", CategoryLearner(history).suggest(txn("Amazon Pay Bill Payment", 1299.0))?.category)
    }

    @Test fun accuracyEstimateNeedsEnoughCorrections() {
        assertNull(CategoryLearner.estimateAccuracy(listOf(txn("A", 1.0, "Rent", CategorySource.USER))))
        val taught = (1..12).flatMap {
            listOf(
                txn("Corner Bakery $it", 120.0, "Food & Dining", CategorySource.USER),
                txn("City Pharmacy $it", 300.0, "Health", CategorySource.USER),
            )
        }
        val acc = CategoryLearner.estimateAccuracy(taught)!!
        assertTrue(acc > 0.8, "accuracy $acc")
    }
}
