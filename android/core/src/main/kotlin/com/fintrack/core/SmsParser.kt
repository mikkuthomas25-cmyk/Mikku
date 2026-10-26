package com.fintrack.core

import java.security.MessageDigest
import java.time.LocalDate

enum class Direction { DEBIT, CREDIT }

data class ParsedTxn(
    val amount: Double,
    val direction: Direction,
    val date: LocalDate,
    val merchant: String,
    val account: String?,
    val ref: String?,
    val smsHash: String,
)

/**
 * Turns bank / UPI / card transaction SMS into [ParsedTxn].
 *
 * Rule-based and tuned for Indian bank formats (Rs./INR/₹, A/c XX1234, UPI VPAs,
 * day-first dates). Anything that isn't a completed transaction (OTPs, promos,
 * payment requests, due reminders, failed payments) returns null.
 */
object SmsParser {
    private val I = RegexOption.IGNORE_CASE

    private val AMOUNT = Regex("""(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)""", I)

    private const val DEBIT_WORDS = "debited|spent|sent|paid|withdrawn|withdrawal|purchase|debit(?:ed)?|txn of|transferred to"
    private const val CREDIT_WORDS = "credited|received|deposited|refund(?:ed)?|reversed|cashback of"
    private val DEBIT = Regex("""\b(?:$DEBIT_WORDS)\b""", I)
    private val CREDIT = Regex("""\b(?:$CREDIT_WORDS)\b""", I)

    private val NOISE = Regex(
        """\botp\b|one[\s-]time password|verification code|""" +
            """will be debited|is due|due date|due on|requested money|collect request|""" +
            """has requested|payment request|minimum amount due|""" +
            """\boffer\b|\bwin\b|\bcongratulations\b|pre-?approved|\bloan of\b|""" +
            """failed|declined|unsuccessful|insufficient""",
        I,
    )

    private val ACCOUNT = Regex(
        """(?:a/c|acct|account|card)\s*(?:no\.?)?\s*(?:(?:ending\s*(?:with|in)?\s*)[x*]*|[x*]+)\s*(\d{3,6})""", I,
    )
    private val REF = Regex(
        """(?:upi\s*ref(?:erence)?|ref(?:erence)?|utr|rrn|txn\s*id)\s*(?:no\.?|num(?:ber)?|#)?\s*[:.\-]?\s*([A-Za-z0-9]{6,})""", I,
    )

    private const val STOP = """(?=\s+on\b|\s+ref\b|\s+upi\b|\s+avl\b|\s+avail|\s+bal\b|\s+via\b|\s+using\b|\s+not\s+you|\s+if\s+not|[.,;()]|\s*-\s|$)"""
    private const val NAME = """([A-Za-z0-9][\w .&'*/@\-]{1,40}?)"""
    private val ATM = Regex("""\batm\b.{0,30}?(?:wdl|withdrawa?l|cash)|(?:cash\s+)?withdrawn\s+at\s+atm|\bat\s+atm\b""", I)
    private val MERCHANT_PATTERNS = listOf(
        Regex("""\b(?:to|from)\s+(?:vpa\s+)?([\w.\-]+@[a-z][\w\-]*)""", I),
        Regex("""\bat\s+$NAME$STOP""", I),
        Regex("""\bto\s+$NAME$STOP""", I),
        Regex("""\bby\s+(?:neft|imps|rtgs|upi)?\s*(?:from\s+)?$NAME$STOP""", I),
        Regex("""\bfrom\s+$NAME$STOP""", I),
        Regex("""\binfo\s*[:\-]\s*([^.]{2,40})""", I),
    )
    // Phrases the merchant patterns can capture that are really the user's own account.
    private val SELF_REF = Regex("""^(?:your\s+)?(?:a/c|acct|account|card|bank|.*\bbank\s+a/?c)\b|^(?:[a-z]+\s+)?bank\b.*[x*]\d""", I)
    private val COMPANY_SUFFIX = Regex("""(?:\s+(?:com|in|india|pvt|private|ltd|limited|llp))+$""", I)

    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private val DATE_ISO = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
    private val DATE_TEXT = Regex("""\b(\d{1,2})[-\s]?([A-Za-z]{3})[a-z]*[-\s,]*(\d{2,4})\b""")
    private val DATE_NUMERIC = Regex("""\b(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})\b""")

    private val LOOKS_BANKISH = Regex("""a/c|acct|account|card|upi|bank""", I)

    fun parse(body: String, receivedOn: LocalDate = LocalDate.now()): ParsedTxn? {
        if (body.isBlank() || NOISE.containsMatchIn(body)) return null
        val amt = AMOUNT.find(body) ?: return null
        val debit = DEBIT.find(body)
        val credit = CREDIT.find(body)
        if (debit == null && credit == null) return null
        // The first verb describes the user's side ("debited from your a/c ... credited to X").
        val direction = if (debit != null && (credit == null || debit.range.first < credit.range.first)) {
            Direction.DEBIT
        } else {
            Direction.CREDIT
        }
        val amount = amt.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0) return null

        return ParsedTxn(
            amount = Math.round(amount * 100) / 100.0,
            direction = direction,
            date = parseDate(body) ?: receivedOn,
            merchant = extractMerchant(body),
            account = ACCOUNT.find(body)?.groupValues?.get(1),
            ref = REF.find(body)?.groupValues?.get(1),
            smsHash = hash(body),
        )
    }

    /** Loose check: is an unparsed SMS worth handing to the AI fallback? */
    fun looksFinancial(body: String): Boolean =
        AMOUNT.containsMatchIn(body) && !NOISE.containsMatchIn(body) && LOOKS_BANKISH.containsMatchIn(body)

    fun hash(body: String): String {
        val normalized = body.trim().lowercase().replace(Regex("""\s+"""), " ")
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    internal fun parseDate(text: String): LocalDate? {
        DATE_ISO.find(text)?.let { m ->
            runCatching { return LocalDate.of(m.int(1), m.int(2), m.int(3)) }
        }
        DATE_TEXT.find(text)?.let { m ->
            val month = MONTHS.indexOf(m.groupValues[2].lowercase())
            if (month >= 0) runCatching { return LocalDate.of(year(m.groupValues[3]), month + 1, m.int(1)) }
        }
        DATE_NUMERIC.find(text)?.let { m ->
            // Indian banks write dates day-first.
            runCatching { return LocalDate.of(year(m.groupValues[3]), m.int(2), m.int(1)) }
        }
        return null
    }

    internal fun extractMerchant(body: String): String {
        if (ATM.containsMatchIn(body)) return "ATM Withdrawal"
        for (pattern in MERCHANT_PATTERNS) {
            for (m in pattern.findAll(body)) {
                val cand = m.groupValues[1].trim()
                if (cand.isEmpty() || SELF_REF.containsMatchIn(cand) || AMOUNT.matchesAt(cand, 0)) continue
                if (cand.all { it.isDigit() || it in " /.-" }) continue
                return cleanMerchant(cand)
            }
        }
        return "Unknown"
    }

    internal fun cleanMerchant(raw: String): String {
        var s = raw.trim(' ', '.', ':', '-', '*')
        if ('@' in s) { // UPI VPA: swiggy.upi@icici -> swiggy
            s = s.substringBefore('@')
            s = s.replace(Regex("""[._-]?(?:upi|pay|payments?)$""", I), "")
            if (s.matches(Regex("""\d{6,}"""))) s = "UPI transfer" // phone-number VPAs
        }
        s = s.replace(Regex("""[._]+"""), " ").replace(Regex("""\s+"""), " ").trim()
        s = s.replace(COMPANY_SUFFIX, "").ifEmpty { s }
        return if (s == s.uppercase() || s == s.lowercase()) titleCase(s) else s
    }

    private fun titleCase(s: String) = s.lowercase().split(" ").joinToString(" ") { w ->
        w.replaceFirstChar { it.uppercaseChar() }
    }

    private fun MatchResult.int(i: Int) = groupValues[i].toInt()
    private fun year(y: String) = y.toInt().let { if (it < 100) it + 2000 else it }
}
