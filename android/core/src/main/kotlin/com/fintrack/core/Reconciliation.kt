package com.fintrack.core

import java.time.LocalDate
import kotlin.math.abs

/** Money the bank balance moved by that FinTrack's transactions don't explain. */
data class BalanceGap(val account: String, val from: LocalDate, val to: LocalDate, val unexplained: Double) {
    /** + means money arrived that FinTrack missed; − means more left than FinTrack recorded missing, or a duplicate. */
    val missedIncome get() = unexplained > 0
}

data class AccountCheck(val account: String, val latestBalance: Double, val latestDate: LocalDate, val checkedSteps: Int, val gaps: List<BalanceGap>) {
    val ok get() = gaps.isEmpty()
}

/**
 * The "mass balance" for SMS: most bank alerts end with the account balance. Between two
 * alerts on the same account, the balance must have moved by exactly the transactions in
 * between. Any difference is a missed transaction (or a duplicate that wasn't removed).
 */
object Reconciliation {
    fun byAccount(txns: List<Txn>): List<AccountCheck> =
        txns.filter { it.account != null && it.source == "sms" && it.effectiveFlow != Flow.DUPLICATE }
            .groupBy { it.account!! }
            .mapNotNull { (account, list) -> check(account, list.sortedWith(compareBy<Txn>({ it.date }, { it.id }))) }
            .sortedBy { it.account }

    private fun check(account: String, list: List<Txn>): AccountCheck? {
        val gaps = mutableListOf<BalanceGap>()
        var anchor: Pair<Txn, Double>? = null
        var moved = 0.0
        var steps = 0
        for (t in list) {
            moved += if (t.direction == Direction.CREDIT) t.amount else -t.amount
            val bal = t.raw?.let { SmsParser.balance(it) } ?: continue
            anchor?.let { (from, start) ->
                steps++
                val diff = bal - (start + moved)
                if (abs(diff) >= 1.0) gaps += BalanceGap(account, from.date, t.date, diff)
            }
            anchor = t to bal
            moved = 0.0
        }
        val last = anchor ?: return null
        return AccountCheck(account, last.second, last.first.date, steps, gaps)
    }
}
