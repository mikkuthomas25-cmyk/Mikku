package com.fintrack.core

import java.time.LocalDate

/**
 * The latest known balance of each account: from a statement's closing balance or an SMS's
 * "Avl Bal", whichever is newer (the statement wins a tie, it's the bank's own record).
 */
object Balances {
    data class Balance(val accountKey: String, val amount: Double, val date: LocalDate, val fromStatement: Boolean)

    /** A statement's closing balance: account (last 4 digits), date of its last row, amount. */
    data class Closing(val account: String, val date: LocalDate, val amount: Double)

    fun latest(txns: List<Txn>, closings: List<Closing>, resolver: Accounts.Resolver = Accounts.Resolver(txns)): List<Balance> {
        val fromSms = txns.asSequence()
            .filter { it.source != "statement" && it.source != "manual" && it.raw != null }
            .mapNotNull { t ->
                val key = resolver.keyOf(t)
                if (key == Accounts.NONE || key == Accounts.MANUAL) return@mapNotNull null
                SmsParser.balance(t.raw!!)?.let { Balance(key, it, t.date, fromStatement = false) }
            }
        val fromStatements = closings.map { Balance("acct:${it.account}", it.amount, it.date, fromStatement = true) }
        return (fromSms + fromStatements)
            .groupBy { it.accountKey }
            .map { (_, list) -> list.maxWith(compareBy<Balance>({ it.date }, { it.fromStatement })) }
            .sortedByDescending { it.amount }
    }
}
