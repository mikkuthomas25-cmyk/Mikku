package com.fintrack.core

import java.time.LocalDate

/** An imported statement: the account (last 4 digits) and the dates it covers. */
data class StatementCover(val account: String, val start: LocalDate, val end: LocalDate) {
    operator fun contains(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)
}

/**
 * A bank statement is the complete, bank-issued record of an account. Once one is imported, it is
 * the record for that account and those dates: every row counts, and the SMS for the same account
 * and dates are set aside (they're the same transactions, read less reliably). Outside the
 * statement's dates, SMS fill in as usual.
 */
object StatementLedger {
    fun apply(txns: List<Txn>, covers: List<StatementCover>, resolver: Accounts.Resolver = Accounts.Resolver(txns)): List<Txn> {
        if (covers.isEmpty()) return txns
        return txns.filter { t ->
            if (t.source == "statement" || t.source == "manual") return@filter true
            val key = resolver.keyOf(t)
            covers.none { c -> key == "acct:${c.account}" && t.date in c }
        }
    }

    /**
     * Where a statement's money went, in the month shown: the statement's own totals, and how
     * FinTrack counted each rupee (spent, given to people, invested, moved to your own accounts,
     * excluded…), so the two can be checked against each other.
     */
    data class Tally(
        val account: String,
        val rows: Int,
        val moneyIn: Double,
        val moneyOut: Double,
        val outBy: Map<Flow, Double>,
        val inBy: Map<Flow, Double>,
    )

    /** [monthTxns] are analysed (their flow set); null when the statement has no rows in them. */
    fun tally(monthTxns: List<Txn>, account: String): Tally? {
        val rows = monthTxns.filter { it.source == "statement" && it.account == account }
        if (rows.isEmpty()) return null
        val (outs, ins) = rows.partition { it.direction == Direction.DEBIT }
        return Tally(
            account = account,
            rows = rows.size,
            moneyIn = ins.sumOf { it.amount },
            moneyOut = outs.sumOf { it.amount },
            outBy = outs.groupBy { it.effectiveFlow }.mapValues { (_, v) -> v.sumOf { it.amount } },
            inBy = ins.groupBy { it.effectiveFlow }.mapValues { (_, v) -> v.sumOf { it.amount } },
        )
    }
}
