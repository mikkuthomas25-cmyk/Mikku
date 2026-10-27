package com.fintrack.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Common Indian bank UPI/card SMS layouts (modelled on real formats; numbers are made up). */
class BankFormatsTest {
    private fun p(body: String) = assertNotNull(SmsParser.parse(body, LocalDate.of(2026, 10, 26)), body)

    @Test fun sbiUpiDebitWithoutCurrency() {
        val t = p("Dear UPI user A/C X1234 debited by 20.0 on date 26Oct26 trf to SWIGGY Refno 629912345678. If not u? call 1800111109. -SBI")
        assertEquals(20.0, t.amount)
        assertEquals(Direction.DEBIT, t.direction)
        assertEquals("Swiggy", t.merchant)
        assertEquals(LocalDate.of(2026, 10, 26), t.date)
        assertEquals("629912345678", t.ref)
    }

    @Test fun sbiUpiCredit() {
        val t = p("Dear SBI UPI User, ur A/cX1234 credited by Rs500 on 25Oct26 by  (Ref no 629987654321)")
        assertEquals(500.0, t.amount)
        assertEquals(Direction.CREDIT, t.direction)
    }

    @Test fun iciciMerchantCredited() {
        val t = p("ICICI Bank Acct XX123 debited for Rs 249.00 on 24-Oct-26; ZOMATO credited. UPI:629911112222. Call 18002662 for dispute. SMS BLOCK 123 to 9215676766.")
        assertEquals(249.0, t.amount)
        assertEquals("Zomato", t.merchant)
    }

    @Test fun axisUpiP2m() {
        val t = p("INR 120.00 debited\nA/c no. XX1234\n23-10-26, 18:22:10\nUPI/P2M/629933334444/BLINKIT\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank")
        assertEquals(120.0, t.amount)
        assertEquals("Blinkit", t.merchant)
        assertEquals(LocalDate.of(2026, 10, 23), t.date)
    }

    @Test fun hdfcMultiLineSent() {
        val t = p("Sent Rs.85.00\nFrom HDFC Bank A/C *5678\nTo CHAI POINT\nOn 22/10/26\nRef 629955556666\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808")
        assertEquals(85.0, t.amount)
        assertEquals("Chai Point", t.merchant)
        assertEquals("5678", t.account)
    }

    @Test fun amountAfterVerbWinsOverBalance() {
        val t = p("Your a/c XX1234 debited by 250.00 on 21-10-26 to VPA uber@axis. Avl Bal Rs 12,000.00")
        assertEquals(250.0, t.amount)
    }

    @Test fun looksFinancialCatchesCurrencylessBankSms() {
        assertTrue(SmsParser.looksFinancial("A/C X1234 debited by 20.0 on date 26Oct26"))
        assertFalse(SmsParser.looksFinancial("123456 is your OTP for login. Do not share."))
    }

    @Test fun redactHidesPersonalNumbersButKeepsAmounts() {
        val r = SmsParser.redact(
            "Rs.1,250.50 debited from A/c XX1234 to VPA rahul.k@okaxis UPI Ref 628555556666. Card ending 4321. Call 9876543210",
        )
        assertTrue("1,250.50" in r, r)
        assertTrue("1234" !in r && "4321" !in r && "628555556666" !in r && "9876543210" !in r, r)
        assertTrue("rahul" !in r && "@okaxis" in r, r)
    }

    @Test fun sbiSalaryAndTransferCredits() {
        val neft = assertNotNull(SmsParser.parse("Dear Customer, Your A/C XXXXX123456 has a credit by NEFT of Rs 44,400.00 on 01/10/26. Avl Bal Rs 50,123.00 -SBI"))
        assertEquals(44400.0, neft.amount)
        assertEquals(Direction.CREDIT, neft.direction)
        val transfer = assertNotNull(SmsParser.parse("Dear SBI User, your A/c X1234-credited by Rs.758 on 22Oct26 transfer from ACME PVT LTD Ref No 612345678901 -SBI"))
        assertEquals(758.0, transfer.amount)
        assertEquals("Acme", transfer.merchant)
        val imps = assertNotNull(SmsParser.parse("Your a/c no. XXXXXXXX1234 is credited by Rs.44,400.00 on 01-10-26 by a/c linked to mobile 9XXXXXX999-ACME LTD (IMPS Ref no 123456789012). -SBI"))
        assertEquals("Acme", imps.merchant)
    }
}
