package com.fintrack.core

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlowTest {
    private var id = 1L
    private val oct = YearMonth.of(2026, 10)
    private fun t(
        day: Int, amount: Double, dir: Direction = Direction.DEBIT, merchant: String = "Shop", category: String = "Other",
        account: String? = "1234", raw: String? = "Rs $amount debited from a/c XX$account to $merchant", ref: String? = null,
        source: String = "sms",
    ) = Txn(id++, oct.atDay(day), amount, dir, merchant, category, account, source, raw, ref = ref)

    @Test fun bankAndUpiAppSmsForOnePaymentCountOnce() {
        val bank = t(5, 640.0, merchant = "Uber", raw = "Rs 640.00 debited from a/c XX1234 on 05-10-26 to VPA uber@axis. Avl Bal Rs 9,000")
        val app = t(5, 640.0, merchant = "Uber", raw = "You paid Rs.640 to Uber India using PhonePe. Txn ID T26100512", account = "1234")
        val a = FlowAnalyzer.analyze(listOf(bank, app))
        assertEquals(Flow.SPEND, a.flowOf(bank))
        assertEquals(Flow.DUPLICATE, a.flowOf(app))
    }

    @Test fun twoRealChaiPaymentsFromTheSameBankAreNotMerged() {
        val first = t(7, 20.0, merchant = "Chai Point", raw = "Rs 20.00 debited from a/c XX1234 on 07-10-26 to VPA chai@ybl UPI Ref 629911110001")
        val second = t(7, 20.0, merchant = "Chai Point", raw = "Rs 20.00 debited from a/c XX1234 on 07-10-26 to VPA chai@ybl UPI Ref 629911110002")
        val a = FlowAnalyzer.analyze(listOf(first, second))
        assertEquals(listOf(Flow.SPEND, Flow.SPEND), listOf(a.flowOf(first), a.flowOf(second)))
    }

    @Test fun moneyMovedBetweenYourAccountsIsNotSpending() {
        val out = t(1, 10000.0, merchant = "Mikku", account = "1234", raw = "Rs 10,000 debited from a/c XX1234 to VPA mikku@okhdfcbank")
        val inn = t(2, 10000.0, Direction.CREDIT, merchant = "Mikku", account = "5678", raw = "Rs 10,000 credited to a/c XX5678 from mikku@okhdfcbank")
        val a = FlowAnalyzer.analyze(listOf(out, inn))
        assertEquals(Flow.OWN_TRANSFER, a.flowOf(out))
        assertEquals(Flow.OWN_TRANSFER, a.flowOf(inn))
        assertEquals(inn.id, a.decisions.getValue(out.id).linkedId)
    }

    @Test fun aFriendPayingBackTheSameAmountIsNotAnOwnTransfer() {
        val paid = t(3, 500.0, merchant = "Rahul", account = "1234", raw = "Rs 500 debited from a/c XX1234 to VPA rahul@okaxis")
        val back = t(4, 500.0, Direction.CREDIT, merchant = "Rahul", account = "1234", raw = "Rs 500 credited to a/c XX1234 from rahul@okaxis")
        val a = FlowAnalyzer.analyze(listOf(paid, back))
        assertEquals(Flow.TO_PEOPLE, a.flowOf(paid))
        assertEquals(Flow.FROM_PEOPLE, a.flowOf(back))
    }

    @Test fun refundsReduceSpendingInTheirCategory() {
        val order = t(8, 900.0, merchant = "Swiggy", category = "Food & Dining")
        val refund = t(10, 300.0, Direction.CREDIT, merchant = "Swiggy", category = "Food & Dining", raw = "Rs 300 credited to a/c XX1234 from Swiggy")
        val a = FlowAnalyzer.analyze(listOf(order, refund))
        assertEquals(Flow.REFUND, a.flowOf(refund))
        val txns = listOf(order, refund).map { it.copy(flow = a.flowOf(it), category = a.decisions[it.id]?.category ?: it.category) }
        val s = Summaries.build(oct, txns, emptyList(), oct.atDay(26))
        assertEquals(600.0, s.spent)
        assertEquals(0.0, s.received)
        assertEquals(Amount("Food & Dining", 600.0, 1), s.byCategory.single())
    }

    @Test fun cardBillsAndInvestmentsAreNotSpending() {
        val bill = t(15, 12430.0, merchant = "ICICI Credit Card", category = "Self Transfer")
        val sip = t(5, 5000.0, merchant = "Groww", category = "Investments")
        val a = FlowAnalyzer.analyze(listOf(bill, sip))
        assertEquals(Flow.OWN_TRANSFER, a.flowOf(bill))
        assertEquals(Flow.INVESTED, a.flowOf(sip))
    }

    @Test fun askShopOrPersonForFrequentPersonalPayees() {
        val kirana = (1..4).map { t(it + 2, 150.0 * it, merchant = "Ramesh Kumar", raw = "Rs ${150 * it} debited to VPA ramesh.k@okicici P2A") }
        val a = FlowAnalyzer.analyze(kirana)
        val q = a.review.filterIsInstance<ReviewItem.ShopOrPerson>().single()
        assertEquals("Ramesh Kumar", q.payee)
        assertEquals(4, q.count)

        // The answer applies to every payment to that payee, and the question goes away.
        val answered = FlowAnalyzer.analyze(kirana, payeeFlows = mapOf(FlowAnalyzer.norm("Ramesh Kumar") to Flow.SPEND))
        assertTrue(kirana.all { answered.flowOf(it) == Flow.SPEND })
        assertTrue(answered.review.isEmpty())
    }

    @Test fun theFlowModelLearnsFromAnswers() {
        // The user said these personal-ID payments to "Anand Tiffin" variants were shop spending.
        val taught = (1..3).map { t(it, 120.0, merchant = "Anand Tiffin Centre", raw = "Rs 120 debited, UPI to anand.t@okaxis") }
        val userFlows = taught.associate { it.id to Flow.SPEND }
        val newOne = t(20, 130.0, merchant = "Anand Tiffin", raw = "Rs 130 debited, UPI to anand.t@okaxis")
        val learner = FlowLearner(taught, userFlows, emptyMap())
        assertEquals(Flow.SPEND, learner.suggest(newOne))
        assertNull(learner.suggest(t(21, 99.0, merchant = "Zqx Unknown Co")))
    }

    @Test fun moneyFlowStatementAddsUp() {
        val txns = listOf(
            t(1, 50000.0, Direction.CREDIT, merchant = "Acme", category = "Income", raw = "Salary credited"),
            t(2, 20000.0, merchant = "Landlord", category = "Rent"),
            t(5, 5000.0, merchant = "Groww", category = "Investments"),
            t(6, 1500.0, merchant = "Rahul", raw = "Rs 1500 to rahul@okaxis"),
        )
        val a = FlowAnalyzer.analyze(txns)
        val s = Summaries.build(oct, txns.map { it.copy(flow = a.flowOf(it)) }, emptyList(), oct.atDay(26))
        assertEquals(50000.0, s.received)
        assertEquals(20000.0, s.spent)
        assertEquals(5000.0, s.byFlow[Flow.INVESTED])
        assertEquals(1500.0, s.byFlow[Flow.TO_PEOPLE])
        assertEquals(50000.0 - 20000 - 5000 - 1500, s.net)
    }

    @Test fun balanceCheckFindsAMissedPayment() {
        val txns = listOf(
            t(1, 1000.0, raw = "Rs 1,000 debited from a/c XX1234. Avl Bal Rs 9,000.00"),
            t(3, 500.0, raw = "Rs 500 debited from a/c XX1234. Avl Bal Rs 8,500.00"),
            // A ₹200 payment that FinTrack never saw happened here.
            t(6, 300.0, raw = "Rs 300 debited from a/c XX1234. Avl Bal Rs 8,000.00"),
        )
        val check = Reconciliation.byAccount(txns).single()
        assertEquals(8000.0, check.latestBalance)
        assertEquals(2, check.checkedSteps)
        val gap = check.gaps.single()
        assertEquals(-200.0, gap.unexplained, 0.001)
        assertEquals(oct.atDay(3), gap.from)
    }

    @Test fun balanceCheckPassesWhenEverythingIsCaptured() {
        val txns = listOf(
            t(1, 1000.0, raw = "Rs 1,000 debited from a/c XX1234. Avl Bal Rs 9,000.00"),
            t(2, 250.0, raw = "Rs 250 debited from a/c XX1234 (no balance in this one)"),
            t(4, 2000.0, Direction.CREDIT, raw = "Rs 2,000 credited to a/c XX1234. Avl Bal Rs 10,750.00"),
        )
        assertTrue(Reconciliation.byAccount(txns).single().ok)
    }

    @Test fun reversalOfAFailedPaymentIsReadAsMoneyBack() {
        val p = assertNotNull(SmsParser.parse("Rs 499.00 for your failed transaction at FLIPKART has been reversed to your a/c XX1234 on 12-10-26"))
        assertEquals(499.0, p.amount)
        assertEquals(Direction.CREDIT, p.direction)
        assertNull(SmsParser.parse("Your transaction of Rs 499 at FLIPKART failed. Please retry."))
        assertEquals(12345.67, SmsParser.balance("Rs 100 debited. Avl Bal Rs 12,345.67"))
        assertNull(SmsParser.balance("Rs 2,499 spent on card XX9876. Avl Lmt: INR 47,501.00"))
    }

    @Test fun whatTheUserExcludesLeavesTheTotals() {
        val salary = t(1, 50000.0, Direction.CREDIT, merchant = "Acme", category = "Income", raw = "Salary credited")
        val rent = t(2, 20000.0, merchant = "Landlord", category = "Rent")
        val oddOne = t(3, 7000.0, merchant = "Someone", raw = "Rs 7000 debited a/c XX1234 to Someone")
        val a = FlowAnalyzer.analyze(listOf(salary, rent, oddOne), userFlows = mapOf(oddOne.id to Flow.IGNORED))
        assertEquals(Flow.IGNORED, a.flowOf(oddOne))
        val s = Summaries.build(oct, listOf(salary, rent, oddOne).map { it.copy(flow = a.flowOf(it)) }, emptyList(), oct.atDay(26))
        assertEquals(20000.0, s.spent)
        assertEquals(30000.0, s.net)
        assertEquals(2, s.txnCount)
    }

    @Test fun aStatementRowForAPaymentAlreadyInSmsCountsOnce() {
        val sms = t(5, 1200.0, merchant = "Zomato", raw = "Rs 1200 debited from a/c XX1234 to VPA zomato@hdfc UPI Ref 512345678901", ref = "512345678901")
        val row = t(6, 1200.0, merchant = "UPI ZOMATO", account = null, raw = "UPI/DR/998877/ZOMATO/HDFC", ref = "998877", source = "statement")
        val a = FlowAnalyzer.analyze(listOf(row, sms))
        assertEquals(Flow.SPEND, a.flowOf(sms))
        assertEquals(Flow.DUPLICATE, a.flowOf(row))
    }

    @Test fun aStatementRowDaysAwayFromItsSmsBecomesAQuestion() {
        val sms = t(5, 1200.0, merchant = "Zomato")
        val row = t(8, 1200.0, merchant = "ZOMATO", account = null, raw = "UPI/DR/ZOMATO", source = "statement")
        val a = FlowAnalyzer.analyze(listOf(sms, row))
        assertEquals(Flow.SPEND, a.flowOf(row))
        assertTrue(a.review.any { it is ReviewItem.PossibleDuplicate })
    }

    @Test fun oneStatementCopyCannotSwallowTwoRealPayments() {
        val first = t(5, 300.0, merchant = "Cafe", raw = "Rs 300 debited from a/c XX1234 to Cafe ref 1")
        val second = t(5, 300.0, merchant = "Cafe", raw = "Rs 300 debited from a/c XX1234 to Cafe ref 2 on another day")
        val row = t(5, 300.0, merchant = "CAFE", account = null, raw = "POS CAFE", source = "statement")
        val a = FlowAnalyzer.analyze(listOf(first, second, row))
        assertEquals(1, listOf(first, second, row).count { a.flowOf(it) == Flow.DUPLICATE })
    }

    @Test fun transferWhereOneSideHasNoAccountIsAskedNotDoubleCounted() {
        val out = t(1, 15000.0, merchant = "Mikku Thomas", account = "1234", raw = "Rs 15000 debited from a/c XX1234 IMPS to MIKKU THOMAS")
        val inn = t(1, 15000.0, Direction.CREDIT, merchant = "Mikku Thomas", account = null, raw = "Rs 15000 credited to your account via IMPS from MIKKU THOMAS")
        val a = FlowAnalyzer.analyze(listOf(out, inn))
        val q = a.review.filterIsInstance<ReviewItem.PossibleOwnTransfer>().single()
        assertEquals(out.id, q.out.id)
        assertEquals(inn.id, q.into.id)
        assertTrue(FlowAnalyzer.analyze(listOf(out, inn), dismissed = setOf(q.key)).review.none { it is ReviewItem.PossibleOwnTransfer })
    }

    @Test fun anSmsNamingYourOtherAccountIsAnOwnTransfer() {
        val card = t(2, 800.0, merchant = "Amazon", account = "9012", raw = "Rs 800 spent on your credit card XX9012 at Amazon")
        val bill = t(20, 15000.0, merchant = "HDFC Bank", account = "1234", raw = "Rs 15000 debited from a/c XX1234 towards HDFC credit card XX9012")
        val a = FlowAnalyzer.analyze(listOf(card, bill))
        assertEquals(Flow.SPEND, a.flowOf(card))
        assertEquals(Flow.OWN_TRANSFER, a.flowOf(bill))
    }

    @Test fun aDebitCardPurchaseIsStillSpending() {
        val card = t(2, 800.0, merchant = "Amazon", account = "9012", raw = "Rs 800 spent on Debit Card XX9012 at Amazon")
        val shop = t(4, 450.0, merchant = "DMart", account = "1234", raw = "Rs 450 debited from a/c XX1234 for POS purchase at DMART using card XX9012")
        assertEquals(Flow.SPEND, FlowAnalyzer.analyze(listOf(card, shop)).flowOf(shop))
    }

    @Test fun walletTopUpIsNotIncome() {
        val load = t(3, 2000.0, Direction.CREDIT, merchant = "Paytm", account = null, raw = "Rs 2000 added to your Paytm Wallet. Balance Rs 2100")
        val a = FlowAnalyzer.analyze(listOf(load))
        assertEquals(Flow.OWN_TRANSFER, a.flowOf(load))
    }

    @Test fun checkReportListsEveryTransactionWithoutAccountNumbers() {
        val spend = t(3, 250.0, merchant = "rahul@okaxis")
        val row = t(4, 99.0, merchant = "Netflix", account = null, raw = "NETFLIX", source = "statement")
        val txns = listOf(spend, row).map { it.copy(flow = Flow.SPEND) }
        val text = Diagnostics.report(oct, txns, emptyMap(), Summaries.build(oct, txns, emptyList(), oct.atDay(26)))
        assertTrue("SMS: 1" in text && "statement: 1" in text, text)
        assertTrue("Spent ₹349" in text, text)
        assertTrue("okaxis" in text && "rahul@" !in text, text)
    }

    @Test fun statementNarrationFindsTheCardBill() {
        assertEquals("Self Transfer", Categorizer.keywordCategory("HDFC0001234", Direction.DEBIT, "NEFT DR HDFC0001234 CC BILL PAYMENT"))
        assertEquals("Other", Categorizer.keywordCategory("HDFC0001234", Direction.DEBIT))
    }

    @Test fun railwayWalletTopUpCountsOnceAsTransport() {
        val topUp = t(3, 3000.0, merchant = "Indian Railways UTS", category = Categorizer.keywordCategory("Indian Railways UTS", Direction.DEBIT))
        val ticket = t(9, 90.0, merchant = "your RWallet Account", account = null, raw = "Rs.90.00 debited from your RWallet Account for UTS ticket")
        val a = FlowAnalyzer.analyze(listOf(topUp, ticket))
        assertEquals("Transport", topUp.category)
        assertEquals(Flow.SPEND, a.flowOf(topUp))
        assertEquals(Flow.OWN_TRANSFER, a.flowOf(ticket))
    }

    @Test fun paymentsToYourOwnNameAreLearnedAsOwnTransfers() {
        val a1 = t(1, 1000.0, merchant = "Mikku - Th", account = null, source = "statement")
        val a2 = t(3, 1639.0, merchant = "MR - Mikku", account = null, source = "statement")
        val later = t(17, 294.0, merchant = "MR - Mikku", account = null, raw = "UPI/DR/mikku@okhdfcbank", source = "statement")
        val friend = t(18, 300.0, merchant = "Rahul", raw = "Rs 300 debited to rahul@okaxis")
        val user = mapOf(a1.id to Flow.OWN_TRANSFER, a2.id to Flow.OWN_TRANSFER)
        // Even an old "person" answer for that payee gives way to the user's own name.
        val a = FlowAnalyzer.analyze(listOf(a1, a2, later, friend), user, payeeFlows = mapOf(FlowAnalyzer.norm("MR - Mikku") to Flow.TO_PEOPLE))
        assertEquals(Flow.OWN_TRANSFER, a.flowOf(later))
        assertEquals(Flow.TO_PEOPLE, a.flowOf(friend))
    }

    @Test fun meAsAPayeeAnswerIsAnOwnTransfer() {
        val p = t(5, 2000.0, merchant = "Mikku Thomas", raw = "Rs 2000 debited to mikku@okicici")
        val a = FlowAnalyzer.analyze(listOf(p), payeeFlows = mapOf(FlowAnalyzer.norm("Mikku Thomas") to Flow.OWN_TRANSFER))
        assertEquals(Flow.OWN_TRANSFER, a.flowOf(p))
    }

    @Test fun chittyAndIndstocksAreInvestments() {
        assertEquals("Investments", Categorizer.keywordCategory("Investment Chitty", Direction.DEBIT))
        assertEquals("Investments", Categorizer.keywordCategory("INDstocks", Direction.DEBIT))
    }
}
