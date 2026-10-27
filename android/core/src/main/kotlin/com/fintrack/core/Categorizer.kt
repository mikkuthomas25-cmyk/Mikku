package com.fintrack.core

object Categorizer {
    val CATEGORIES = listOf(
        "Food & Dining", "Groceries", "Shopping", "Transport", "Fuel",
        "Bills & Utilities", "Rent", "Entertainment", "Health", "Education",
        "Travel", "Cash Withdrawal", "Investments", "Transfers", "Self Transfer", "Income", "Daily small spends", "Other",
    )

    /**
     * Money moving between the user's own accounts (e.g. paying the credit card
     * bill from savings). Counting it would double-count the card purchases.
     */
    val EXCLUDED_FROM_TOTALS = setOf("Self Transfer")

    /** Built-in brand list; also the learner's starting knowledge. */
    val KEYWORDS: Map<String, List<String>> = linkedMapOf(
        "Self Transfer" to listOf("credit card", "card payment", "cc payment", "cc bill", "card bill",
            "self transfer", "own account", "cred club", "cred", "slice"),
        "Food & Dining" to listOf("swiggy", "zomato", "eatsure", "domino", "pizza", "mcdonald", "kfc",
            "starbucks", "cafe", "restaurant", "hotel", "burger", "chai", "bakery", "dunzo"),
        "Groceries" to listOf("bigbasket", "blinkit", "grofers", "zepto", "instamart", "dmart", "jiomart",
            "more retail", "reliance fresh", "spencer", "nature basket", "kirana", "supermarket"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "tatacliq", "croma",
            "reliance digital", "decathlon", "ikea", "lifestyle", "westside", "mall"),
        "Transport" to listOf("uber", "ola", "rapido", "metro", "irctc", "redbus", "namma yatri", "fastag", "parking",
            "railway", "railways", "uts", "ksrtc"),
        "Fuel" to listOf("petrol", "fuel", "hpcl", "bpcl", "iocl", "indian oil", "shell", "bharat petroleum"),
        "Bills & Utilities" to listOf("electricity", "bescom", "tneb", "msedcl", "airtel", "jio", "vodafone", "vi",
            "bsnl", "broadband", "act fibernet", "gas", "water", "recharge", "dth", "tata play", "billdesk",
            "insurance", "lic"),
        "Rent" to listOf("rent", "landlord", "nobroker", "housing"),
        "Entertainment" to listOf("netflix", "hotstar", "prime video", "spotify", "youtube", "bookmyshow",
            "pvr", "inox", "sony liv", "zee5", "steam", "playstation"),
        "Health" to listOf("pharmacy", "apollo", "medplus", "1mg", "pharmeasy", "netmeds", "hospital",
            "clinic", "diagnostic", "practo", "cult fit", "cultfit", "gym"),
        "Education" to listOf("udemy", "coursera", "byju", "unacademy", "school", "college", "university", "fees"),
        "Travel" to listOf("makemytrip", "goibibo", "cleartrip", "indigo", "air india", "vistara", "akasa",
            "spicejet", "oyo", "airbnb", "booking.com", "yatra"),
        "Cash Withdrawal" to listOf("atm withdrawal", "atm"),
        "Investments" to listOf("zerodha", "groww", "upstox", "indmoney", "kuvera", "coin", "mutual fund",
            "sip", "nps", "ppf", "smallcase", "indstocks", "investment", "chit", "chitty", "chitti", "kuri"),
    )

    private val PATTERNS: List<Pair<String, Regex>> = KEYWORDS.flatMap { (cat, words) ->
        words.map { w -> cat to Regex("""(?<![a-z])${Regex.escape(w)}(?![a-z])""") }
    }

    /**
     * [context]: more text to look in when the name alone says nothing, e.g. a statement row's full
     * narration ("NEFT CC BILL PAYMENT HDFC…") whose shortened name lost the clue.
     */
    fun keywordCategory(merchant: String, direction: Direction, context: String? = null): String {
        for (text in listOfNotNull(merchant, context)) {
            val m = text.lowercase()
            if (direction == Direction.CREDIT && "interest" in m) return "Income"
            // Money coming in is never a cash withdrawal.
            PATTERNS.firstOrNull { it.second.containsMatchIn(m) && !(direction == Direction.CREDIT && it.first == "Cash Withdrawal") }
                ?.let { return it.first }
        }
        return if (direction == Direction.CREDIT) "Income" else "Other"
    }

    /**
     * Unknown payees under this amount (tea, snacks, the local shop over UPI) go to [SMALL] instead
     * of "Other", so nobody has to sort dozens of tiny payments by hand. Bigger unknowns stay "Other".
     */
    const val SMALL_LIMIT = 500.0
    const val SMALL = "Daily small spends"

    /** [category] as found by the brand list and the model; replaces a small unknown debit's "Other" with [SMALL]. */
    fun withSmallSpends(category: String, direction: Direction, amount: Double): String =
        if (category == "Other" && direction == Direction.DEBIT && amount <= SMALL_LIMIT) SMALL else category

    /** Statement narrations are worth searching; SMS bodies aren't ("spent on your credit card" isn't a card bill). */
    fun contextOf(t: Txn): String? = t.raw.takeIf { t.source == "statement" }

    /** [rules] maps a lowercased merchant name to the category the user picked for it before. */
    fun categorize(merchant: String, direction: Direction, rules: Map<String, String>): String =
        categorizeWithSource(merchant, direction, rules).first

    /** Category plus where it came from: "rule" (the user's choice) or "keyword" (built-in list). */
    fun categorizeWithSource(merchant: String, direction: Direction, rules: Map<String, String>, context: String? = null): Pair<String, String> =
        rules[merchant.lowercase()]?.let { it to CategorySource.RULE } ?: (keywordCategory(merchant, direction, context) to CategorySource.KEYWORD)
}
