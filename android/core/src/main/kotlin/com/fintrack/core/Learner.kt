package com.fintrack.core

import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log2

/** Where a transaction's category came from. */
object CategorySource {
    const val USER = "user"        // the user picked it for this transaction
    const val RULE = "rule"        // the user's "always use this for <merchant>"
    const val KEYWORD = "keyword"  // built-in brand list (or "Other" when nothing matched)
    const val LEARNED = "ml"       // guessed by the on-device model
}

/** Turns a transaction into the signals the model learns from. */
object Features {
    private val NON_ALNUM = Regex("[^a-z0-9]+")
    private val PERSONAL_HANDLE = Regex("@(ok[a-z]+)")
    private val ANY_HANDLE = Regex("@([a-z]+)")

    fun of(merchant: String, direction: Direction, amount: Double, date: LocalDate?, raw: String?): List<String> = buildList {
        add("dir:$direction")
        val m = merchant.lowercase().replace(NON_ALNUM, " ").trim()
        if (m.isNotEmpty() && m != "unknown") {
            m.split(' ').filter { it.length > 1 }.forEach { add("w:$it") }
            // Character trigrams match spelling variants: "swiggy" ~ "swiggy instamart" ~ "swiggy*order".
            val padded = " $m "
            for (i in 0..padded.length - 3) padded.substring(i, i + 3).takeIf { it.isNotBlank() }?.let { add("c:$it") }
        }
        if (amount > 0) {
            add("amt:${log2(amount).toInt()}")  // ₹64–127 → 6, ₹8k–16k → 13 …
            if (amount >= 500 && amount % 500 == 0.0) add("amt:round")
        }
        date?.let {
            add("dow:${it.dayOfWeek.value}")
            add("dom:" + when (it.dayOfMonth) { in 1..5 -> "start"; in 26..31 -> "end"; else -> "mid" })
        }
        raw?.lowercase()?.let { r ->
            if ("p2a" in r) add("raw:p2a")             // UPI to a person
            if ("p2m" in r) add("raw:p2m")             // UPI to a merchant
            PERSONAL_HANDLE.find(r)?.let { add("raw:personal-upi") }  // @okaxis, @oksbi …: GPay personal IDs
            ANY_HANDLE.find(r)?.let { add("raw:psp:${it.groupValues[1]}") }
            if ("card" in r) add("raw:card")
            if ("atm" in r) add("raw:atm")
            if ("neft" in r || "imps" in r || "rtgs" in r) add("raw:bank-transfer")
            if ("upi" in r) add("raw:upi")
        }
    }

    fun of(t: Txn) = of(t.merchant, t.direction, t.amount, t.date, t.raw)

    /** Features that describe the merchant itself (not amount or timing). */
    fun isMerchant(feature: String) = feature.startsWith("w:") || feature.startsWith("c:")
}

data class Example(val features: List<String>, val label: String, val weight: Double)

data class Suggestion(val category: String, val confidence: Double)

/** Multinomial Naive Bayes with add-α smoothing. Small, fast, and trains in milliseconds. */
class NaiveBayes(private val alpha: Double = 0.5) {
    private val classWeight = HashMap<String, Double>()
    private val featureWeight = HashMap<String, HashMap<String, Double>>()
    private val classTotal = HashMap<String, Double>()
    private val vocab = HashSet<String>()

    val classes: Set<String> get() = classWeight.keys

    fun train(examples: Iterable<Example>) {
        for (ex in examples) {
            classWeight.merge(ex.label, ex.weight, Double::plus)
            val fw = featureWeight.getOrPut(ex.label) { HashMap() }
            for (f in ex.features) {
                fw.merge(f, ex.weight, Double::plus)
                classTotal.merge(ex.label, ex.weight, Double::plus)
                vocab += f
            }
        }
    }

    /** How much weight [label] has seen for [feature] (0 when never). */
    fun weight(label: String, feature: String): Double = featureWeight[label]?.get(feature) ?: 0.0

    /** Probability for every class, highest first. */
    fun probabilities(features: List<String>): List<Pair<String, Double>> {
        if (classWeight.isEmpty()) return emptyList()
        val total = classWeight.values.sum()
        val v = vocab.size.toDouble()
        val known = features.filter { it in vocab }
        val logs = classWeight.keys.associateWith { c ->
            val fw = featureWeight[c].orEmpty()
            val denom = (classTotal[c] ?: 0.0) + alpha * v
            ln(classWeight.getValue(c) / total) + known.sumOf { ln(((fw[it] ?: 0.0) + alpha) / denom) }
        }
        val max = logs.values.max()
        val exps = logs.mapValues { exp(it.value - max) }
        val z = exps.values.sum()
        return exps.map { it.key to it.value / z }.sortedByDescending { it.second }
    }
}

/**
 * Learns the user's categories from their own history.
 *
 * Training data: the built-in brand list (weak), transactions categorized by that list, and
 * — weighted highest — every category the user picked themselves. The model's own guesses are
 * never trained on, so a wrong guess can't reinforce itself.
 */
class CategoryLearner(labelled: List<Txn>) {
    private val model = NaiveBayes()
    val userTaught: Int = labelled.count { it.categorySource == CategorySource.USER || it.categorySource == CategorySource.RULE }

    init {
        model.train(seedExamples())
        model.train(labelled.filter(::isTrusted).map { t ->
            Example(Features.of(t), t.category, weightFor(t.categorySource))
        })
    }

    /** A category guess, or null when the model isn't confident or has never seen anything like this merchant. */
    fun suggest(t: Txn, minConfidence: Double = MIN_CONFIDENCE): Suggestion? {
        val features = Features.of(t)
        val (category, p) = model.probabilities(features).firstOrNull() ?: return null
        if (p < minConfidence || category == "Other") return null
        // Amount and timing alone are not enough: a real share of the name must match what this
        // category has learned (or, for payments to people, the UPI signals must).
        val nameParts = features.filter(Features::isMerchant)
        val matched = nameParts.count { model.weight(category, it) > 0 }
        val personSignal = category == "Transfers" &&
            ("raw:personal-upi" in features || "raw:p2a" in features) && model.weight(category, "raw:personal-upi") + model.weight(category, "raw:p2a") > 0
        if (!personSignal && (nameParts.isEmpty() || matched.toDouble() / nameParts.size < MIN_NAME_MATCH)) return null
        if (category == "Income" && t.direction == Direction.DEBIT) return null
        return Suggestion(category, p)
    }

    companion object {
        const val MIN_CONFIDENCE = 0.6
        const val MIN_NAME_MATCH = 0.3

        /**
         * Labels worth learning from. "Other" and the default "Income" on every credit are
         * fallbacks, not real labels, and the model's own guesses must never train it.
         */
        fun isTrusted(t: Txn) = when (t.categorySource) {
            CategorySource.USER, CategorySource.RULE -> t.category != "Other"
            CategorySource.KEYWORD -> t.category != "Other" && !(t.category == "Income" && t.direction == Direction.CREDIT)
            else -> false
        }

        /** Rows the model may (re)categorize: its own past guesses and the keyword fallbacks. */
        fun isLearnable(t: Txn) = t.categorySource == CategorySource.LEARNED ||
            (t.categorySource == CategorySource.KEYWORD && !isTrusted(t))

        fun weightFor(source: String) = when (source) {
            CategorySource.USER, CategorySource.RULE -> 5.0
            else -> 1.0
        }

        private fun seedExamples(): List<Example> = Categorizer.KEYWORDS.flatMap { (category, words) ->
            words.map { w -> Example(Features.of(w, Direction.DEBIT, 0.0, null, null), category, 0.5) }
        }

        /**
         * Accuracy on the user's own corrections, by k-fold cross-validation: the share of
         * corrections the model would have guessed right without being told. Null below [minLabels].
         */
        fun estimateAccuracy(all: List<Txn>, folds: Int = 5, minLabels: Int = 10): Double? {
            val taught = all.filter {
                (it.categorySource == CategorySource.USER || it.categorySource == CategorySource.RULE) && it.category != "Other"
            }
            if (taught.size < minLabels) return null
            var right = 0
            var asked = 0
            taught.indices.groupBy { it % folds }.values.forEach { testIdx ->
                val testIds = testIdx.map { taught[it].id }.toSet()
                val learner = CategoryLearner(all.filter { it.id !in testIds })
                testIdx.forEach { i ->
                    asked++
                    if (learner.suggest(taught[i], minConfidence = 0.0)?.category == taught[i].category) right++
                }
            }
            return right.toDouble() / asked
        }
    }
}
