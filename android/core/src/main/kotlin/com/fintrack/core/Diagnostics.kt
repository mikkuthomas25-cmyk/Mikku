package com.fintrack.core

import java.time.YearMonth

/**
 * "Check my numbers": a plain-text report of one month that the user can share, showing where
 * every rupee in the totals came from (SMS, statement, added by hand) and how FinTrack treated it.
 * Account numbers, phone numbers and UPI IDs are hidden, as for missed messages.
 */
object Diagnostics {
    fun sourceOf(t: Txn) = when (t.source) {
        "statement" -> "statement"
        "manual" -> "added by hand"
        "shared" -> "shared SMS"
        MissingMoney.SOURCE -> "balance check"
        else -> if (Notifications.isNotification(t.source)) t.source.removePrefix("notification:") + " notification" else "SMS"
    }

    fun report(month: YearMonth, txns: List<Txn>, decisions: Map<Long, FlowDecision>, summary: MonthSummary?): String = buildString {
        val rows = txns.filter { YearMonth.from(it.date) == month }.sortedWith(compareBy<Txn>({ it.date }, { it.id }))
        appendLine("FinTrack check · ${month.month.name.lowercase().replaceFirstChar(Char::uppercase)} ${month.year}")
        if (summary != null) {
            appendLine("Money in ${amount(summary.received)} · Spent ${amount(summary.spent)} · Left ${amount(summary.net)}")
        }
        appendLine()
        appendLine("Where the transactions came from:")
        rows.groupBy(::sourceOf).forEach { (src, list) -> appendLine("  $src: ${list.size}") }
        appendLine()
        appendLine("How they were treated:")
        rows.groupBy { it.effectiveFlow }.toSortedMap().forEach { (flow, list) ->
            val label = if (flow.counted) flow.label else "${flow.label} (not counted)"
            appendLine("  $label: ${list.size} · ${amount(list.sumOf { it.amount })}")
        }
        appendLine()
        appendLine("date | in/out | amount | counted as | category | from | name | why")
        for (t in rows) {
            val why = decisions[t.id]?.reason?.takeIf { it.isNotBlank() } ?: "default"
            appendLine(
                listOf(
                    t.date.toString(),
                    if (t.direction == Direction.DEBIT) "out" else "in",
                    amount(t.amount),
                    t.effectiveFlow.label,
                    t.category,
                    sourceOf(t),
                    SmsParser.redact(t.merchant),
                    SmsParser.redact(why),
                ).joinToString(" | "),
            )
        }
    }

    private fun amount(v: Double) = "₹" + if (v % 1.0 == 0.0) v.toLong().toString() else "%.2f".format(v)
}
