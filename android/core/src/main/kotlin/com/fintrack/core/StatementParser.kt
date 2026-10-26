package com.fintrack.core

import java.time.LocalDate
import kotlin.math.abs

/** One transaction row read from a bank statement. */
data class StatementRow(
    val date: LocalDate,
    val narration: String,
    val amount: Double,
    val direction: Direction,
    val balance: Double?,
    /** True when the running balance confirmed this row's amount and direction. */
    val verified: Boolean,
) {
    val merchant: String get() = Narration.merchant(narration)
    val ref: String? get() = Narration.ref(narration)

    /** Stable id so importing the same statement twice adds nothing. */
    val key: String get() = SmsParser.hash("stmt|$date|$amount|$direction|${balance ?: ""}|$narration")
}

/**
 * The statement's "mass balance": opening + money in − money out must equal closing, and every
 * row must move the running balance by exactly its amount.
 */
data class BalanceCheck(
    val opening: Double?,
    val closing: Double?,
    val totalIn: Double,
    val totalOut: Double,
    /** Rows where the running balance didn't move by the row's amount (likely misread). */
    val brokenRows: List<StatementRow>,
) {
    val expectedClosing: Double? get() = opening?.let { it + totalIn - totalOut }
    val difference: Double? get() = if (closing != null && expectedClosing != null) closing - expectedClosing!! else null
    val balanced: Boolean get() = brokenRows.isEmpty() && (difference?.let { abs(it) < 0.01 } ?: true)
}

data class StatementResult(val rows: List<StatementRow>, val check: BalanceCheck) {
    /** No transaction rows at all: usually a scanned (image) PDF or an unsupported layout. */
    val empty get() = rows.isEmpty()
}

/**
 * Reads transactions from a bank statement's text (as extracted from a PDF or CSV).
 *
 * Works from the parts every statement shares rather than any one bank's layout: rows start with
 * a date and end with amounts, the last amount being the running balance. The balance chain then
 * decides whether each row is money in or out, and doubles as a check that it was read correctly.
 */
object StatementParser {
    private val I = RegexOption.IGNORE_CASE
    private const val DATE = """(\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4}|\d{1,2}[\s\-][A-Za-z]{3}[\s\-,]*\d{2,4})"""
    private val ROW_START = Regex("""^\s*$DATE\b""")
    private val SECOND_DATE = Regex("""^\s*$DATE\s+""")
    // 1,23,456.78 · 1,234.50 · 950.00, optionally followed by Cr/Dr.
    private val MONEY = Regex("""(?<![\d.,])(\d{1,3}(?:,\d{2,3})*\.\d{2}|\d+\.\d{2})(?:\s*(Cr|Dr)\b)?""", I)
    private val OPENING = Regex("""opening\s+balance""", I)
    private val CLOSING = Regex("""closing\s+balance""", I)
    private val NOISE = Regex(
        """^\s*(page\s+\d|date\s+(narration|description|particulars|details)|statement\s+(of|summary|period)|""" +
            """account\s+(no|number|summary)|customer\s+id|generated\s+on|this\s+is\s+a\s+computer)""",
        I,
    )
    private val CREDIT_HINT = Regex("""\b(cr|credit|credited|deposit|refund|reversal|salary|interest|neft\s*cr|/cr/|imps\s*cr)\b""", I)
    private val DATE_NUMERIC = Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})""")
    private val DATE_TEXT = Regex("""(\d{1,2})[\s\-]([A-Za-z]{3})[\s\-,]*(\d{2,4})""")
    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

    private data class Raw(val date: LocalDate, var narration: String, val amounts: List<Pair<Double, String?>>)

    fun parse(text: String): StatementResult {
        val lines = text.replace(' ', ' ').lines().map { it.trim() }.filter { it.isNotEmpty() }
        val opening = lines.firstOrNull { OPENING.containsMatchIn(it) }?.let { lastMoney(it) }
        val closingLine = lines.lastOrNull { CLOSING.containsMatchIn(it) }?.let { lastMoney(it) }

        val raws = mutableListOf<Raw>()
        for (line in lines) {
            if (NOISE.containsMatchIn(line) || OPENING.containsMatchIn(line) || CLOSING.containsMatchIn(line)) continue
            val start = ROW_START.find(line)
            val money = MONEY.findAll(line).toList()
            if (start != null && money.isNotEmpty()) {
                val date = parseDate(start.groupValues[1]) ?: continue
                var rest = line.substring(start.range.last + 1)
                SECOND_DATE.find(rest)?.let { rest = rest.substring(it.range.last + 1) } // value date column
                val firstMoney = MONEY.find(rest)
                val narration = (if (firstMoney != null) rest.substring(0, firstMoney.range.first) else rest).trim()
                val amounts = MONEY.findAll(rest).map { m ->
                    m.groupValues[1].replace(",", "").toDouble() to m.groupValues[2].ifEmpty { null }?.uppercase()
                }.toList()
                raws += Raw(date, narration, amounts)
            } else if (start == null && money.isEmpty() && raws.isNotEmpty() && line.length < 120) {
                // Narrations often wrap onto the next line(s).
                raws.last().narration = (raws.last().narration + " " + line).trim()
            }
        }

        // Most statements list oldest first; some newest first. Keep whichever order the balances agree with.
        val (rows, broken) = listOf(chain(raws, opening), chain(raws.asReversed(), opening))
            .minBy { it.second.size }

        val effectiveOpening = opening ?: rows.firstOrNull()?.let { first ->
            first.balance?.let { if (first.direction == Direction.DEBIT) it + first.amount else it - first.amount }
        }
        val closing = closingLine ?: rows.lastOrNull { it.balance != null }?.balance
        return StatementResult(
            rows,
            BalanceCheck(
                opening = effectiveOpening,
                closing = closing,
                totalIn = rows.filter { it.direction == Direction.CREDIT }.sumOf { it.amount },
                totalOut = rows.filter { it.direction == Direction.DEBIT }.sumOf { it.amount },
                brokenRows = broken,
            ),
        )
    }

    private fun chain(raws: List<Raw>, opening: Double?): Pair<List<StatementRow>, List<StatementRow>> {
        val rows = mutableListOf<StatementRow>()
        val broken = mutableListOf<StatementRow>()
        var prev: Double? = opening
        for (r in raws) {
            if (r.amounts.isEmpty()) continue
            val balance = if (r.amounts.size >= 2) signed(r.amounts.last()) else null
            // With separate Withdrawal/Deposit columns one is blank, so the amount sits just before the balance.
            val amount = if (r.amounts.size >= 2) r.amounts[r.amounts.size - 2].first else r.amounts.first().first
            if (amount <= 0) continue
            val byChain = when {
                prev == null || balance == null -> null
                abs(prev - amount - balance) < 0.01 -> Direction.DEBIT
                abs(prev + amount - balance) < 0.01 -> Direction.CREDIT
                else -> null
            }
            val row = StatementRow(r.date, r.narration, amount, byChain ?: guessDirection(r), balance, verified = byChain != null)
            if (byChain == null && prev != null && balance != null) broken += row
            rows += row
            prev = balance ?: prev
        }
        return rows to broken
    }

    /** Uses explicit Cr/Dr markers or wording when the balance chain can't decide (e.g. the first row). */
    private fun guessDirection(r: Raw): Direction {
        val marker = r.amounts.getOrNull(r.amounts.size - 2)?.second ?: if (r.amounts.size == 1) r.amounts[0].second else null
        return when {
            marker == "CR" -> Direction.CREDIT
            marker == "DR" -> Direction.DEBIT
            CREDIT_HINT.containsMatchIn(r.narration) -> Direction.CREDIT
            else -> Direction.DEBIT
        }
    }

    /** "1,000.00 Dr" on a balance means overdrawn (negative). */
    private fun signed(p: Pair<Double, String?>) = if (p.second == "DR") -p.first else p.first

    private fun lastMoney(line: String) = MONEY.findAll(line).lastOrNull()?.let { m ->
        val v = m.groupValues[1].replace(",", "").toDouble()
        if (m.groupValues[2].equals("Dr", true)) -v else v
    }

    internal fun parseDate(s: String): LocalDate? {
        fun year(y: String) = y.toInt().let { if (it < 100) it + 2000 else it }
        DATE_NUMERIC.find(s)?.let { m ->
            return runCatching { LocalDate.of(year(m.groupValues[3]), m.groupValues[2].toInt(), m.groupValues[1].toInt()) }.getOrNull()
        }
        DATE_TEXT.find(s)?.let { m ->
            val month = MONTHS.indexOf(m.groupValues[2].lowercase())
            if (month >= 0) return runCatching { LocalDate.of(year(m.groupValues[3]), month + 1, m.groupValues[1].toInt()) }.getOrNull()
        }
        return null
    }
}

/** Pulls a merchant name and UPI reference out of a statement narration. */
object Narration {
    private val I = RegexOption.IGNORE_CASE
    private val UPI_SLASH = Regex("""\bUPI/(?:DR|CR|P2[AM])/(\d+)/([^/]+)""", I)        // UPI/DR/412345678901/SWIGGY/...
    private val UPI_DASH = Regex("""\bUPI-([^-]+)-([\w.\-]+@[\w]+)?""", I)               // UPI-SWIGGY-swiggy@icici-...
    private val POS = Regex("""\bPOS\s+[\dX*]+\s+([A-Za-z][A-Za-z .&'*\-]{1,40}?)(?=\s+\d|\s*$)""", I) // POS 4XXX1234 AMAZON PAY
    private val TRANSFER = Regex("""\b(?:NEFT|IMPS|RTGS)\b[\s\-/:]*(?:CR|DR)?[\s\-/:]*(?:[A-Z]{4}0\w{6}[\s\-/:]*)?(?:\d+[\s\-/:]*)?([A-Za-z][A-Za-z .&]{2,40})""", I)
    private val ATM = Regex("""\b(ATM|NWD|CASH\s*WDL|ATW)\b""", I)
    private val UPI_REF = Regex("""(?<!\d)(\d{12})(?!\d)""")

    fun merchant(narration: String): String {
        if (ATM.containsMatchIn(narration)) return "ATM Withdrawal"
        val raw = UPI_SLASH.find(narration)?.groupValues?.get(2)
            ?: UPI_DASH.find(narration)?.groupValues?.get(1)
            ?: POS.find(narration)?.groupValues?.get(1)
            ?: TRANSFER.find(narration)?.groupValues?.get(1)
            ?: narration.replace(Regex("""[\d/\-:*]+"""), " ").trim().split(Regex("""\s+""")).take(3).joinToString(" ")
        return SmsParser.cleanMerchant(raw).ifBlank { "Unknown" }
    }

    /** The 12-digit UPI/IMPS reference, used to match statement rows with SMS already captured. */
    fun ref(narration: String): String? = UPI_REF.find(narration)?.groupValues?.get(1)
}
