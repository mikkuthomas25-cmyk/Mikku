package com.fintrack.core

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * The catch-all for payments that came without an SMS or a notification (a swiped-away Google Pay
 * alert, an app that sends nothing): a bank balance can't miss anything. Between two known balances
 * of the same account (an SMS "Avl Bal", a statement's closing balance, or one the user set), the
 * balance must have moved by exactly the payments FinTrack saw in between. Any difference is money
 * that came in or went out unseen.
 */
object MissingMoney {
    /** [amount] > 0: money arrived unseen; < 0: money left unseen. */
    data class Gap(val accountKey: String, val from: LocalDate, val to: LocalDate, val amount: Double) {
        val key get() = "gap:$accountKey:$to:${(amount * 100).roundToLong()}"
        val incoming get() = amount > 0
    }

    /** Where a known balance sits among the transactions (those after it aren't in it yet). */
    private data class Reading(val date: LocalDate, val seq: Long, val amount: Double)

    /** Source of transactions recorded from a balance difference: they sit before that day's readings. */
    const val SOURCE = "balance"

    /** Order of a transaction within its day; one recorded from a balance check comes first. */
    fun seq(t: Txn) = if (t.source == SOURCE) 0L else t.id

    fun find(
        txns: List<Txn>,
        closings: List<Balances.Closing>,
        userBalances: List<Balances.UserBalance>,
        isDuplicate: (Txn) -> Boolean,
        dismissed: Set<String> = emptySet(),
        resolver: Accounts.Resolver = Accounts.Resolver(txns),
        minAmount: Double = 1.0,
    ): List<Gap> {
        val counted = txns.filter { it.source != "manual" && !isDuplicate(it) }
        val readings = HashMap<String, MutableList<Reading>>()
        counted.filter { it.source != "statement" && it.raw != null }.forEach { t ->
            val key = resolver.keyOf(t)
            if (key != Accounts.NONE && key != Accounts.MANUAL) {
                SmsParser.balance(t.raw!!)?.let { readings.getOrPut(key) { mutableListOf() } += Reading(t.date, seq(t), it) }
            }
        }
        closings.forEach { readings.getOrPut("acct:${it.account}") { mutableListOf() } += Reading(it.date, Long.MAX_VALUE, it.amount) }
        userBalances.forEach { readings.getOrPut(it.accountKey) { mutableListOf() } += Reading(it.date, it.anchorId, it.amount) }

        val byKey = counted.groupBy(resolver::keyOf)
        val order = compareBy<Pair<LocalDate, Long>>({ it.first }, { it.second })
        return readings.flatMap { (key, list) ->
            val sorted = list.sortedWith(compareBy({ it.date }, { it.seq }))
            val moves = byKey[key].orEmpty()
            sorted.zipWithNext().mapNotNull { (prev, cur) ->
                val between = moves.filter {
                    val p = it.date to seq(it)
                    order.compare(p, prev.date to prev.seq) > 0 && order.compare(p, cur.date to cur.seq) <= 0
                }
                val expected = prev.amount + between.sumOf { if (it.direction == Direction.CREDIT) it.amount else -it.amount }
                val diff = cur.amount - expected
                if (abs(diff) < minAmount) null else Gap(key, prev.date, cur.date, Math.round(diff * 100) / 100.0)
            }
        }.filter { it.key !in dismissed }.sortedByDescending { it.to }
    }
}
