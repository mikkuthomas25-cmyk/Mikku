package com.fintrack.core

import java.time.LocalDate

/**
 * Each account's balance, kept up to date: start from the newest known balance (a statement's
 * closing balance, an SMS "Avl Bal", or one the user entered) and apply every payment in or out
 * that arrived after it. Duplicates (the same payment from an SMS and an app notification) and cash
 * entries don't move a bank balance.
 */
object Balances {
    enum class Source { STATEMENT, SMS, YOU }

    /** [adjustedBy]: payments applied since the [date] reading. */
    data class Balance(
        val accountKey: String,
        val amount: Double,
        val date: LocalDate,
        val source: Source,
        val adjustedBy: Int = 0,
    ) {
        val fromStatement get() = source == Source.STATEMENT
    }

    /** A statement's closing balance: account (last 4 digits), date of its last row, amount. */
    data class Closing(val account: String, val date: LocalDate, val amount: Double)

    /** A balance the user entered for [accountKey] on [date]; transactions up to [anchorId] were already in it. */
    data class UserBalance(val accountKey: String, val date: LocalDate, val amount: Double, val anchorId: Long)

    /** One known balance, and where it sits among transactions (later ones still need applying). */
    private data class Reading(val key: String, val amount: Double, val date: LocalDate, val seq: Long, val source: Source)

    /** The newest known balance of each account, without rolling forward. */
    fun latest(txns: List<Txn>, closings: List<Closing>, resolver: Accounts.Resolver = Accounts.Resolver(txns)): List<Balance> =
        current(txns, closings, emptyList(), { false }, resolver).map { it.copy(adjustedBy = 0) }

    fun current(
        txns: List<Txn>,
        closings: List<Closing>,
        userBalances: List<UserBalance>,
        isDuplicate: (Txn) -> Boolean,
        resolver: Accounts.Resolver = Accounts.Resolver(txns),
    ): List<Balance> {
        val readings = buildList {
            txns.filter { it.source != "statement" && it.source != "manual" && it.raw != null }.forEach { t ->
                val key = resolver.keyOf(t)
                if (key != Accounts.NONE && key != Accounts.MANUAL) SmsParser.balance(t.raw!!)?.let { add(Reading(key, it, t.date, t.id, Source.SMS)) }
            }
            // A statement's closing balance covers its whole last day.
            closings.forEach { add(Reading("acct:${it.account}", it.amount, it.date, Long.MAX_VALUE, Source.STATEMENT)) }
            userBalances.forEach { add(Reading(it.accountKey, it.amount, it.date, it.anchorId, Source.YOU)) }
        }
        return readings.groupBy { it.key }.map { (key, list) ->
            val r = list.maxWith(compareBy<Reading>({ it.date }, { it.seq }))
            val later = txns.filter { t ->
                t.source != "manual" && !isDuplicate(t) && resolver.keyOf(t) == key &&
                    (t.date > r.date || (t.date == r.date && t.id > r.seq))
            }
            val moved = later.sumOf { if (it.direction == Direction.CREDIT) it.amount else -it.amount }
            Balance(key, r.amount + moved, r.date, r.source, later.size)
        }.sortedByDescending { it.amount }
    }
}
