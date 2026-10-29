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
    /** Bump when parsing improves: the app then re-reads old SMS once to pick up what it missed. */
    const val VERSION = 5

    private val I = RegexOption.IGNORE_CASE

    private val AMOUNT = Regex("""(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)""", I)
    // "debited by 20.0", "credited with Rs.500", "Sent Rs.120": the amount right after the verb.
    // Preferred over the first Rs/INR in the text, which is sometimes the balance.
    private val AMOUNT_AFTER_VERB = Regex(
        """\b(?:debited|credited|deducted|spent|paid|sent|received|withdrawn|credit\s+by\s+\w+\s+of)\s+(?:by|for|with|of)?\s*(?:rs\.?|inr|₹)?\s*([\d,]*\d(?:\.\d{1,2})?)(?![\d/-])""",
        I,
    )

    private const val DEBIT_WORDS = "debited|spent|sent|paid|withdrawn|withdrawal|purchase|debit(?:ed)?|txn of|transferred to"
    // SBI: "Your A/C XX1234 has a credit by NEFT of Rs 44,400.00"
    // "Rahul paid you ₹500" (Google Pay, PhonePe) is money in, though it starts with "paid".
    private const val CREDIT_WORDS = "paid you|sent you|transferred to you|credited|received|deposited|refund(?:ed)?|reversed|cashback of|has a credit|credit of"
    private val DEBIT = Regex("""\b(?:$DEBIT_WORDS)\b""", I)
    private val CREDIT = Regex("""\b(?:$CREDIT_WORDS)\b""", I)

    // Never a completed payment: OTP codes (not just "never share your OTP"), failed payments,
    // payment requests, money that "will be" debited.
    private val HARD_NOISE = Regex(
        """\botp\s*(?:is|:|-)\s*\d|\b\d{4,8}\s+is\s+(?:the\s+|your\s+)?(?:otp|one[\s-]time password|verification code)|""" +
            """one[\s-]time password\s+(?:is|for)|verification code\s+(?:is|:)|otp\s+for\s+(?:txn|transaction)|""" +
            """will be debited|requested money|collect request|has requested|payment request|""" +
            """failed|declined|unsuccessful|insufficient""",
        I,
    )
    // Offers, reminders and dues: noise only when the message doesn't also report money that moved
    // ("A/c debited Rs 500. Your EMI is due on 5th" is a payment).
    private val SOFT_NOISE = Regex(
        """is due|due date|due on|minimum amount due|\boffer\b|\bwin\b|\bcongratulations\b|pre-?approved|\bloan of\b""",
        I,
    )
    private val MONEY_MOVED = Regex(
        """\b(?:debited|credited|spent|withdrawn|deducted|deposited|received|transferred|paid|sent|has a credit)\b""",
        I,
    )

    /** Not a completed transaction (an OTP code, a failed payment, an offer or a due reminder). */
    private fun isNoise(body: String): Boolean =
        !REVERSAL.containsMatchIn(body) &&
            (HARD_NOISE.containsMatchIn(body) || (SOFT_NOISE.containsMatchIn(body) && !MONEY_MOVED.containsMatchIn(body)))

    private val ACCOUNT = Regex(
        """(?:a/c|acct|account|card)\s*(?:no\.?)?\s*(?:(?:ending\s*(?:with|in)?\s*)[x*]*|[x*]+)\s*(\d{3,6})""", I,
    )
    private val REF = Regex(
        """\b(?:upi\s*ref(?:erence)?|ref(?:erence)?|utr|rrn|txn\s*id)\s*(?:no\.?|num(?:ber)?|#)?\s*[:.\-]?\s*([A-Za-z0-9]{6,})""", I,
    )

    private const val STOP = """(?=\s+on\b|\s+ref|\s+upi\b|\s+avl\b|\s+avail|\s+bal\b|\s+via\b|\s+using\b|\s+not\s+you|\s+if\s+not|[.,;()]|\s*-\s|$)"""
    private const val NAME = """([A-Za-z0-9][\w .&'*/@\-]{1,40}?)"""
    private val ATM = Regex("""\batm\b.{0,30}?(?:wdl|withdrawa?l|cash)|(?:cash\s+)?withdrawn\s+at\s+atm|\bat\s+atm\b""", I)
    private val MERCHANT_PATTERNS = listOf(
        // Google Pay / PhonePe: "Rahul Kumar paid you ₹500", "Rahul sent you ₹500"
        Regex("""(?:^|[.!]\s*)([A-Za-z][\w .&'\-]{1,40}?)\s+(?:paid|sent)\s+you\b""", I),
        // SBI: "credited by Rs.44400 on 01Oct26 transfer from ACME PVT LTD Ref No …"
        Regex("""\btransfer\s+from\s+$NAME$STOP""", I),
        // SBI IMPS: "by a/c linked to mobile 9XXXXXX999-ACME LTD (IMPS Ref …)"
        Regex("""linked\s+to\s+mobile\s+[\dx*]+\s*-\s*([A-Za-z][\w .&'\-]{1,40}?)$STOP""", I),
        // "UPI/P2M/412345678901/MERCHANT NAME/..." (Axis and others)
        Regex("""\bUPI/P2[AM]/\d+/([^/\n]{2,40})""", I),
        // ICICI: "...debited for Rs 20.00 on 26-Oct-26; MERCHANT credited."
        Regex(""";\s*([A-Za-z0-9][\w .&'*/@\-]{1,40}?)\s+credited\b""", I),
        Regex("""\b(?:to|from)\s+(?:vpa\s+)?([\w.\-]+@[a-z][\w\-]*)""", I),
        Regex("""\btowards\s+(?!your\b)$NAME$STOP""", I),
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

    // "Rs 500 for failed txn has been reversed/refunded/credited back": money coming back.
    private val REVERSAL = Regex("""\b(reversed|refunded|credited\s+back|reversal)\b""", I)
    // "Avl Bal Rs 12,345.67", "Available balance: INR 9,000", "Bal Rs.500" (not card limits).
    private val BALANCE = Regex(
        """\b(?:avl\.?|avail(?:able)?\.?|a/c|clear)?\s*bal(?:ance)?\.?\s*(?:is|:|-)?\s*(?:rs\.?|inr|₹)\s*(-?[\d,]+(?:\.\d{1,2})?)""",
        I,
    )

    /** The account balance an SMS reports after the transaction, if any. */
    fun balance(body: String): Double? = BALANCE.find(body)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

    private val LOOKS_BANKISH = Regex("""a/c|acct|account|card|upi|bank""", I)

    fun parse(body: String, receivedOn: LocalDate = LocalDate.now()): ParsedTxn? {
        if (body.isBlank() || isNoise(body)) return null
        val amt = AMOUNT_AFTER_VERB.find(body) ?: AMOUNT.find(body) ?: return null
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
        (AMOUNT.containsMatchIn(body) || AMOUNT_AFTER_VERB.containsMatchIn(body)) &&
            !isNoise(body) && LOOKS_BANKISH.containsMatchIn(body)

    /**
     * Hides account, card, reference and phone numbers and UPI IDs, so a message can be shared
     * to improve the parser without revealing personal details. Amounts and wording stay.
     */
    fun redact(body: String): String = body
        .replace(Regex("""[\w.\-]+@([a-z][\w\-]*)""", I), "•••@$1")
        .replace(Regex("""(?<=[x*])\d{3,}""", I), "••••")
        .replace(Regex("""(?<=ending\s(?:with\s|in\s)?)\d{3,}""", I), "••••")
        .replace(Regex("""(?<!\d|\d[.,])\d{6,}(?!\d|[.,]\d)"""), "••••••")

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

    // "Transferred to Mr. RAHUL KUMAR": the full stop after Mr would end the name at "Mr".
    private val HONORIFIC = Regex("""\b(?:mr|mrs|ms|dr|shri|smt)\.\s*""", I)

    internal fun extractMerchant(original: String): String {
        if (ATM.containsMatchIn(original)) return "ATM Withdrawal"
        val body = original.replace(HONORIFIC, "")
        for (pattern in MERCHANT_PATTERNS) {
            for (m in pattern.findAll(body)) {
                val cand = m.groupValues[1].trim()
                if (cand.isEmpty() || SELF_REF.containsMatchIn(cand) || AMOUNT.matchesAt(cand, 0)) continue
                if (cand.lowercase().trimEnd('.') in setOf("rs", "inr")) continue  // "credited by Rs.500": that's the amount
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
