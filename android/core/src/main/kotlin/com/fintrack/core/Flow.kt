package com.fintrack.core

import kotlin.math.abs

/**
 * What a transaction means for your money, separate from its category. Only [SPEND] (minus
 * [REFUND]s) counts as spending; [OWN_TRANSFER] and [DUPLICATE] don't count at all.
 */
enum class Flow(val label: String) {
    SPEND("Spending"),
    INCOME("Income"),
    REFUND("Refund"),
    INVESTED("Invested / saved"),
    TO_PEOPLE("Sent to people"),
    FROM_PEOPLE("Received from people"),
    OWN_TRANSFER("Between your accounts"),
    DUPLICATE("Duplicate"),
    ;

    val countsAsMoneyIn get() = this == INCOME || this == FROM_PEOPLE
    val counted get() = this != OWN_TRANSFER && this != DUPLICATE

    companion object {
        /** Simple fallback when no analysis has run: what every transaction was before flows existed. */
        fun basic(t: Txn): Flow = when {
            t.category in Categorizer.EXCLUDED_FROM_TOTALS -> OWN_TRANSFER
            t.direction == Direction.DEBIT -> SPEND
            else -> INCOME
        }
    }
}

/** The flow a transaction ends up with, and why (shown to the user). */
data class FlowDecision(
    val flow: Flow,
    val reason: String,
    /** The other half of a pair: the duplicate kept, the matching own-account credit, the refunded spend. */
    val linkedId: Long? = null,
    /** Set for refunds: the category of the spend being refunded, so it reduces the right category. */
    val category: String? = null,
)

/** A question for the user; their answer is stored and teaches the model. */
sealed interface ReviewItem {
    val key: String

    /** Two transactions that look like the same payment. */
    data class PossibleDuplicate(val keep: Txn, val duplicate: Txn) : ReviewItem {
        override val key get() = "dup:${keep.id}:${duplicate.id}"
    }

    /** Someone paid often via a personal UPI ID: a shop (spending) or a person? */
    data class ShopOrPerson(val payee: String, val count: Int, val total: Double) : ReviewItem {
        override val key get() = "payee:${payee.lowercase()}"
    }
}

data class FlowAnalysis(val decisions: Map<Long, FlowDecision>, val review: List<ReviewItem>) {
    fun flowOf(t: Txn) = decisions[t.id]?.flow ?: Flow.basic(t)
}

/**
 * Works out each transaction's [Flow] from the whole history.
 *
 * Order of evidence: the user's own answers; facts that can be proven from the data (the same
 * reference twice, a debit matched by an equal credit into another account, a refund matched to
 * its spend); the built-in signals (UPI payments to personal IDs, investment and card-bill
 * categories); then a learned model for what's left, which only acts when confident. Uncertain
 * cases become [ReviewItem]s instead of guesses.
 */
object FlowAnalyzer {
    private val REFUND_WORDS = Regex("""\b(refund(ed)?|revers(al|ed)|cashback|chargeback)\b""", RegexOption.IGNORE_CASE)
    private val MERCHANT_PAYMENT = Regex("""\b(p2m|pos|spent on|purchase)\b""", RegexOption.IGNORE_CASE)
    private val PERSONAL = Regex("""\bp2a\b|@ok[a-z]+""", RegexOption.IGNORE_CASE)
    private val DIGITS = Regex("""[\d.,]+""")

    /**
     * @param userFlows flows the user set on individual transactions.
     * @param payeeFlows the user's answers per payee (e.g. "Ramesh Kumar" → SPEND because it's a shop).
     * @param dismissed review keys the user already answered or dismissed.
     */
    fun analyze(
        txns: List<Txn>,
        userFlows: Map<Long, Flow> = emptyMap(),
        payeeFlows: Map<String, Flow> = emptyMap(),
        dismissed: Set<String> = emptySet(),
    ): FlowAnalysis {
        val sorted = txns.sortedWith(compareBy<Txn>({ it.date }, { it.id }))
        val out = HashMap<Long, FlowDecision>()
        val review = mutableListOf<ReviewItem>()
        for (t in sorted) userFlows[t.id]?.let { out[t.id] = FlowDecision(it, "Set by you") }

        // 1. Duplicates: same direction and amount within a day, reported differently.
        val byKey = sorted.filter { it.id !in out }.groupBy { it.direction to Math.round(it.amount * 100) }
        for (group in byKey.values) {
            if (group.size < 2) continue
            for (i in group.indices) for (j in i + 1 until group.size) {
                val a = group[i]
                val b = group[j]
                if (a.id in out || b.id in out) continue
                if (abs(a.date.toEpochDay() - b.date.toEpochDay()) > 1) continue
                when (duplicateConfidence(a, b)) {
                    Confidence.CERTAIN, Confidence.HIGH ->
                        out[b.id] = FlowDecision(Flow.DUPLICATE, "Same payment as ${a.merchant} on ${a.date}", linkedId = a.id)
                    Confidence.MEDIUM -> ReviewItem.PossibleDuplicate(a, b).takeIf { it.key !in dismissed }?.let { review += it }
                    Confidence.NONE -> Unit
                }
            }
        }

        // 2. Between your own accounts: card bills, and a debit matched by an equal credit into a
        //    *different* account. FinTrack only sees SMS for your own accounts, so both ends being
        //    known, different accounts is the proof.
        for (t in sorted) {
            if (t.id !in out && t.category in Categorizer.EXCLUDED_FROM_TOTALS) out[t.id] = FlowDecision(Flow.OWN_TRANSFER, "Card bill / own account")
        }
        val credits = sorted.filter { it.direction == Direction.CREDIT && it.id !in out }.toMutableList()
        for (d in sorted.filter { it.direction == Direction.DEBIT && it.id !in out }) {
            if (d.raw != null && MERCHANT_PAYMENT.containsMatchIn(d.raw)) continue
            val c = credits.firstOrNull {
                abs(it.amount - d.amount) < 0.005 && it.date.toEpochDay() - d.date.toEpochDay() in 0..2 &&
                    it.account != null && d.account != null && it.account != d.account && !looksLikeSalary(it)
            } ?: continue
            credits -= c
            out[d.id] = FlowDecision(Flow.OWN_TRANSFER, "Matched by ${rupeesPlain(c.amount)} arriving on ${c.date}", linkedId = c.id)
            out[c.id] = FlowDecision(Flow.OWN_TRANSFER, "Matched by ${rupeesPlain(d.amount)} leaving on ${d.date}", linkedId = d.id)
        }

        // 3. Refunds: money back from somewhere you spent (not a person paying you back), or worded as
        //    a refund / reversal.
        val spendsByMerchant = sorted.filter {
            it.direction == Direction.DEBIT && it.id !in out && !isPersonal(it)
        }.groupBy { norm(it.merchant) }
        for (c in sorted.filter { it.direction == Direction.CREDIT && it.id !in out }) {
            val original = spendsByMerchant[norm(c.merchant)]
                ?.filter { it.date <= c.date && c.date.toEpochDay() - it.date.toEpochDay() <= 60 && it.amount + 0.005 >= c.amount }
                ?.maxByOrNull { it.date }
            val worded = REFUND_WORDS.containsMatchIn(c.raw ?: "") || REFUND_WORDS.containsMatchIn(c.merchant)
            if (original != null && c.merchant != "Unknown" && !isPersonal(c)) {
                out[c.id] = FlowDecision(Flow.REFUND, "Money back from ${original.merchant}", linkedId = original.id, category = original.category)
            } else if (worded) {
                out[c.id] = FlowDecision(Flow.REFUND, "Marked as a refund or reversal", category = c.category.takeIf { it != "Income" })
            }
        }

        // 4. Investments and people.
        for (t in sorted) {
            if (t.id in out) continue
            val answer = payeeFlows[norm(t.merchant)]
            if (answer != null) {
                out[t.id] = FlowDecision(adjust(answer, t.direction), "You said ${t.merchant} is ${if (answer == Flow.SPEND) "a shop" else "a person"}")
                continue
            }
            if (t.category == "Investments") {
                out[t.id] = FlowDecision(if (t.direction == Direction.DEBIT) Flow.INVESTED else Flow.INCOME, "Investment")
                continue
            }
            if (isPersonal(t)) {
                out[t.id] = FlowDecision(
                    if (t.direction == Direction.DEBIT) Flow.TO_PEOPLE else Flow.FROM_PEOPLE,
                    "UPI to a personal ID",
                )
            }
        }

        // 5. Learned: what the user's past answers say about the rest. Only when confident.
        val learner = FlowLearner(sorted, userFlows, payeeFlows)
        for (t in sorted) {
            if (t.id in out) continue
            val guess = learner.suggest(t)
            out[t.id] = guess?.let { FlowDecision(it, "Learned from your answers") }
                ?: FlowDecision(if (t.direction == Direction.DEBIT) Flow.SPEND else Flow.INCOME, "")
        }

        // 6. Questions: frequent payments to personal IDs nobody has classified yet.
        sorted.filter { out[it.id]?.flow == Flow.TO_PEOPLE && userFlows[it.id] == null && norm(it.merchant) !in payeeFlows }
            .groupBy { it.merchant }
            .filter { (_, v) -> v.size >= 3 || v.sumOf { it.amount } >= 2000 }
            .map { (payee, v) -> ReviewItem.ShopOrPerson(payee, v.size, v.sumOf { it.amount }) }
            .filter { it.key !in dismissed }
            .sortedByDescending { it.total }
            .let { review += it }

        return FlowAnalysis(out, review)
    }

    private enum class Confidence { CERTAIN, HIGH, MEDIUM, NONE }

    private fun duplicateConfidence(a: Txn, b: Txn): Confidence {
        if (a.ref != null && b.ref != null) return if (a.ref == b.ref) Confidence.CERTAIN else Confidence.NONE
        if (a.account != null && b.account != null && a.account != b.account) return Confidence.NONE
        val ra = a.raw ?: return Confidence.NONE
        val rb = b.raw ?: return Confidence.NONE
        // The same template twice (e.g. two ₹20 chai SMS from one bank) means two real payments.
        if (similarity(ra, rb) >= 0.7) return Confidence.NONE
        val sameMerchant = norm(a.merchant) == norm(b.merchant) || a.merchant == "Unknown" || b.merchant == "Unknown"
        return when {
            a.date == b.date && (a.account != null && a.account == b.account) && sameMerchant -> Confidence.HIGH
            a.date == b.date && sameMerchant && a.source != b.source -> Confidence.HIGH
            else -> Confidence.MEDIUM
        }
    }

    private fun similarity(a: String, b: String): Double {
        fun tokens(s: String) = s.lowercase().replace(DIGITS, " ").split(Regex("""\W+""")).filter { it.length > 1 }.toSet()
        val ta = tokens(a)
        val tb = tokens(b)
        if (ta.isEmpty() || tb.isEmpty()) return 0.0
        return (ta intersect tb).size.toDouble() / (ta union tb).size
    }

    private fun isPersonal(t: Txn) = (t.raw != null && PERSONAL.containsMatchIn(t.raw)) || t.category == "Transfers"

    private fun looksLikeSalary(t: Txn) = t.category == "Income" && Regex("""salary|payroll""", RegexOption.IGNORE_CASE).containsMatchIn(t.raw ?: t.merchant)

    private fun adjust(f: Flow, d: Direction) = when {
        f == Flow.SPEND && d == Direction.CREDIT -> Flow.REFUND
        f == Flow.TO_PEOPLE && d == Direction.CREDIT -> Flow.FROM_PEOPLE
        f == Flow.FROM_PEOPLE && d == Direction.DEBIT -> Flow.TO_PEOPLE
        else -> f
    }

    fun norm(merchant: String) = merchant.lowercase().replace(Regex("""[^a-z0-9]"""), "")

    private fun rupeesPlain(v: Double) = "₹" + if (v % 1.0 == 0.0) v.toLong().toString() else "%.2f".format(v)
}

/**
 * The second learned model: predicts a transaction's [Flow] from the user's own answers
 * (per transaction and per payee), using the same signals as the category model. Never trained
 * on its own guesses; only answers when confident and the payee looks like one it learned.
 */
class FlowLearner(history: List<Txn>, userFlows: Map<Long, Flow>, payeeFlows: Map<String, Flow>) {
    private val model = NaiveBayes()
    private val trained: Boolean

    init {
        val examples = history.mapNotNull { t ->
            val label = userFlows[t.id] ?: payeeFlows[FlowAnalyzer.norm(t.merchant)] ?: return@mapNotNull null
            Example(Features.of(t), label.name, 5.0)
        }
        trained = examples.size >= 3
        model.train(examples)
    }

    fun suggest(t: Txn, minConfidence: Double = 0.7): Flow? {
        if (!trained) return null
        val features = Features.of(t)
        val (label, p) = model.probabilities(features).firstOrNull() ?: return null
        if (p < minConfidence) return null
        val nameParts = features.filter(Features::isMerchant)
        if (nameParts.isEmpty() || nameParts.count { model.weight(label, it) > 0 }.toDouble() / nameParts.size < 0.3) return null
        val flow = Flow.valueOf(label)
        // A learned flow must fit the money's direction.
        val fits = when (flow) {
            Flow.SPEND, Flow.INVESTED, Flow.TO_PEOPLE -> t.direction == Direction.DEBIT
            Flow.INCOME, Flow.REFUND, Flow.FROM_PEOPLE -> t.direction == Direction.CREDIT
            Flow.OWN_TRANSFER, Flow.DUPLICATE -> false  // only from proof or the user, never guessed
        }
        return flow.takeIf { fits }
    }
}
