package com.fintrack.core

import java.time.LocalDate

/**
 * The bank accounts a phone's messages talk about. A phone often gets SMS for accounts that aren't
 * the user's money to track: a family member's, a friend's (e.g. an IOB account a friend uses), an
 * old one. The user picks which accounts count; the rest are left out of everything.
 */
object Accounts {
    /** One account seen in the messages. [key] identifies it in the user's choice. */
    data class Info(
        val key: String,
        val bank: String?,
        val last4: String?,
        val count: Int,
        val balance: Double?,
        val balanceDate: LocalDate?,
    ) {
        val label get() = if (key == NONE) "Statements and messages without an account number"
            else listOfNotNull(bank ?: "Account", last4?.let { "••$it" }).joinToString(" ")
    }

    private val I = RegexOption.IGNORE_CASE
    private val BANKS = listOf(
        "SBI" to Regex("""\bSBI\b|state bank""", I),
        "IOB" to Regex("""\bIOB\b|indian overseas""", I),
        "Slice" to Regex("""\bslice\b""", I),
        "HDFC" to Regex("""\bHDFC\b""", I),
        "ICICI" to Regex("""\bICICI\b""", I),
        "Axis" to Regex("""\baxis\s*bank\b|\bAxis\b""", I),
        "Kotak" to Regex("""\bkotak\b""", I),
        "Canara" to Regex("""\bcanara\b""", I),
        "Federal" to Regex("""\bfederal\s*bank\b|\bFedBank\b""", I),
        "PNB" to Regex("""\bPNB\b|punjab national""", I),
        "Bank of Baroda" to Regex("""\bbank of baroda\b|\bBoB\b""", I),
        "Union Bank" to Regex("""\bunion bank\b""", I),
        "Bank of India" to Regex("""\bbank of india\b|\bBOI\b""", I),
        "Indian Bank" to Regex("""\bindian bank\b""", I),
        "South Indian Bank" to Regex("""\bsouth indian bank\b|\bSIB\b""", I),
        "IDFC First" to Regex("""\bIDFC\b""", I),
        "Yes Bank" to Regex("""\byes\s*bank\b""", I),
        "IndusInd" to Regex("""\bindusind\b""", I),
        "AU Bank" to Regex("""\bAU\s*(small finance\s*)?bank\b""", I),
        "Kerala Gramin" to Regex("""\bkerala gramin\b|\bKGB\b""", I),
        "Paytm Payments Bank" to Regex("""\bpaytm payments bank\b""", I),
        "Airtel Payments Bank" to Regex("""\bairtel payments bank\b""", I),
    )

    fun bankOf(t: Txn): String? {
        val text = t.raw ?: return null
        return BANKS.firstOrNull { it.second.containsMatchIn(text) }?.first
    }

    /**
     * Which account each transaction belongs to: by account number when the message has one;
     * otherwise by bank, when the user has exactly one account at that bank in the messages.
     */
    class Resolver(txns: List<Txn>) {
        private val numberedByBank: Map<String, Set<String>> = txns
            .mapNotNull { t -> t.account?.takeLast(4)?.let { last -> bankOf(t)?.let { it to last } } }
            .groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }

        fun keyOf(t: Txn): String {
            t.account?.takeLast(4)?.let { return "acct:$it" }
            if (t.source == "manual") return MANUAL
            val bank = bankOf(t) ?: return NONE
            val numbered = numberedByBank[bank]
            return if (numbered != null && numbered.size == 1) "acct:${numbered.first()}" else "bank:$bank"
        }
    }

    const val NONE = "none"
    const val MANUAL = "manual"

    /** Every account in [txns], most transactions first. Manual entries are always tracked, so they aren't listed. */
    fun list(txns: List<Txn>): List<Info> {
        val r = Resolver(txns)
        return txns.filter { it.source != "manual" }.groupBy(r::keyOf).map { (key, list) ->
            val withBalance = list.mapNotNull { t -> t.raw?.let(SmsParser::balance)?.let { t to it } }.maxByOrNull { it.first.date }
            Info(
                key = key,
                bank = list.mapNotNull(::bankOf).groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
                last4 = key.removePrefix("acct:").takeIf { key.startsWith("acct:") },
                count = list.size,
                balance = withBalance?.second,
                balanceDate = withBalance?.first?.date,
            )
        }.sortedByDescending { it.count }
    }

    /** [txns] without those in accounts the user chose not to track. */
    fun tracked(txns: List<Txn>, untracked: Set<String>, resolver: Resolver = Resolver(txns)): List<Txn> =
        if (untracked.isEmpty()) txns else txns.filter { resolver.keyOf(it) !in untracked }
}
