package com.fintrack.core

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Statement layouts modelled on common Indian bank PDFs (all numbers made up). */
class StatementParserTest {
    // Date | Narration | Ref | Value date | Withdrawal | Deposit | Balance — oldest first, one column blank per row.
    private val hdfcLike = """
        Statement of account
        Opening Balance 12,750.00
        Date Narration Chq./Ref.No. Value Dt Withdrawal Amt. Deposit Amt. Closing Balance
        01/10/26 UPI-SWIGGY-swiggy@icici-ICIC0000001-629912345601-Payment 0000629912345601 01/10/26 250.00 12,500.00
        01/10/26 NEFT CR-HDFC0000001-ACME TECHNOLOGIES PVT LTD-SALARY OCT 0000N260100001 01/10/26 50,000.00 62,500.00
        03/10/26 UPI-LANDLORD SHARMA-sharma@okaxis-SBIN0000001-629912345602-Rent 0000629912345602 03/10/26 15,000.00 47,500.00
        05/10/26 POS 4XXXXXXXXXXX1234 AMAZON PAY INDIA 0000000000000 05/10/26 2,499.00 45,001.00
        07/10/26 ATW-512345XXXXXX1234-S1ANBG12-BANGALORE 0000000000000 07/10/26 2,000.00 43,001.00
        09/10/26 UPI-BLINKIT-blinkit.payu@hdfcbank-HDFC0000001-629912345603 0000629912345603 09/10/26 1,180.50 41,820.50
        Page 1 of 1
        Closing Balance 41,820.50
    """.trimIndent()

    @Test fun readsRowsAndVerifiesEveryOneByTheRunningBalance() {
        val r = StatementParser.parse(hdfcLike)
        assertEquals(6, r.rows.size)
        assertTrue(r.rows.all { it.verified })
        assertEquals(listOf(Direction.DEBIT, Direction.CREDIT, Direction.DEBIT, Direction.DEBIT, Direction.DEBIT, Direction.DEBIT),
            r.rows.map { it.direction })
        assertEquals(listOf("Swiggy", "Acme Technologies", "Landlord Sharma", "Amazon Pay", "ATM Withdrawal", "Blinkit"),
            r.rows.map { it.merchant })
        assertEquals("629912345601", r.rows[0].ref)
        assertEquals(LocalDate.of(2026, 10, 3), r.rows[2].date)
    }

    @Test fun massBalanceAddsUp() {
        val c = StatementParser.parse(hdfcLike).check
        assertEquals(12750.0, c.opening)
        assertEquals(41820.50, c.closing)
        assertEquals(50000.0, c.totalIn)
        assertEquals(250.0 + 15000 + 2499 + 2000 + 1180.50, c.totalOut, 0.001)
        assertEquals(0.0, c.difference!!, 0.001)
        assertTrue(c.balanced)
    }

    @Test fun flagsAMisreadRowAndAnUnbalancedStatement() {
        // The Amazon row's amount is wrong (2,400 instead of 2,499): the balance chain must catch it.
        val bad = hdfcLike.replace("05/10/26 2,499.00 45,001.00", "05/10/26 2,400.00 45,001.00")
        val c = StatementParser.parse(bad).check
        assertFalse(c.balanced)
        assertEquals(1, c.brokenRows.size)
        assertEquals("Amazon Pay", c.brokenRows.single().merchant)
        assertEquals(-99.0, c.difference!!, 0.001)
    }

    @Test fun handlesNewestFirstStatementsWithWrappedNarrations() {
        // Date | Description | Debit | Credit | Balance — newest first, narration wrapped across lines.
        val sbiLike = """
            Txn Date Description Debit Credit Balance
            12 Oct 2026 UPI/DR/629955550003/ZOMATO/YESB/zomato@ybl/Order 320.00 9,180.00
            10 Oct 2026 UPI/CR/629955550002/RAHUL K/SBIN/rahul.k@oksbi/ 500.00 9,500.00
            split dinner
            08 Oct 2026 UPI/DR/629955550001/BIGBASKET/HDFC/bigbasket@hdfcbank 1,000.00 9,000.00
            Opening Balance 10,000.00
        """.trimIndent()
        val r = StatementParser.parse(sbiLike)
        assertEquals(3, r.rows.size)
        assertTrue(r.rows.all { it.verified }, r.rows.toString())
        val byMerchant = r.rows.associateBy { it.merchant }
        assertEquals(Direction.CREDIT, byMerchant.getValue("Rahul K").direction)
        assertEquals(Direction.DEBIT, byMerchant.getValue("Zomato").direction)
        assertTrue("split dinner" in byMerchant.getValue("Rahul K").narration)
        assertTrue(r.check.balanced)
    }

    @Test fun usesCrDrMarkersWhenThereIsNoBalanceToChainFrom() {
        val text = """
            15-10-2026 IMPS-629977770001-PRIYA S-Refund 1,250.00 Cr
            16-10-2026 UPI/DR/629977770002/UBER/AXIS/uber@axis 410.00 Dr
        """.trimIndent()
        val r = StatementParser.parse(text)
        assertEquals(listOf(Direction.CREDIT, Direction.DEBIT), r.rows.map { it.direction })
        assertFalse(r.rows.any { it.verified })
    }

    @Test fun reimportingGivesTheSameKeys() {
        assertEquals(StatementParser.parse(hdfcLike).rows.map { it.key }, StatementParser.parse(hdfcLike).rows.map { it.key })
    }

    // ---- end to end through a real PDF ------------------------------------------------------

    private fun pdf(lines: List<String>, password: String? = null): ByteArray {
        val doc = PDDocument()
        val page = PDPage()
        doc.addPage(page)
        PDPageContentStream(doc, page).use { cs ->
            cs.beginText()
            cs.setFont(PDType1Font.HELVETICA, 7f)
            cs.setLeading(11f)
            cs.newLineAtOffset(30f, 760f)
            lines.forEach { cs.showText(it); cs.newLine() }
            cs.endText()
        }
        if (password != null) {
            doc.protect(StandardProtectionPolicy("owner-secret", password, AccessPermission()).apply { encryptionKeyLength = 128 })
        }
        val out = ByteArrayOutputStream()
        doc.save(out)
        doc.close()
        return out.toByteArray()
    }

    private fun extract(bytes: ByteArray, password: String = ""): String =
        PDDocument.load(bytes, password).use { PDFTextStripper().apply { sortByPosition = true }.getText(it) }

    @Test fun readsAPdfStatementEndToEnd() {
        val r = StatementParser.parse(extract(pdf(hdfcLike.lines())))
        assertEquals(6, r.rows.size)
        assertTrue(r.check.balanced)
    }

    @Test fun passwordProtectedPdfNeedsThePassword() {
        val bytes = pdf(hdfcLike.lines(), password = "MIKK0101")
        assertFailsWith<InvalidPasswordException> { extract(bytes) }
        assertEquals(6, StatementParser.parse(extract(bytes, "MIKK0101")).rows.size)
    }
}
