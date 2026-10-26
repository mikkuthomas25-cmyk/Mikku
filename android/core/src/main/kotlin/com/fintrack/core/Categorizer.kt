package com.fintrack.core

object Categorizer {
    val CATEGORIES = listOf(
        "Food & Dining", "Groceries", "Shopping", "Transport", "Fuel",
        "Bills & Utilities", "Rent", "Entertainment", "Health", "Education",
        "Travel", "Cash Withdrawal", "Investments", "Transfers", "Self Transfer", "Income", "Other",
    )

    /**
     * Money moving between the user's own accounts (e.g. paying the credit card
     * bill from savings). Counting it would double-count the card purchases.
     */
    val EXCLUDED_FROM_TOTALS = setOf("Self Transfer")

    /** Built-in brand list; also the learner's starting knowledge. */
    val KEYWORDS: Map<String, List<String>> = linkedMapOf(
        "Self Transfer" to listOf("credit card", "card payment", "cc payment", "cc bill", "card bill",
            "self transfer", "own account", "cred club", "cred"),
        "Food & Dining" to listOf("swiggy", "zomato", "eatsure", "domino", "pizza", "mcdonald", "kfc",
            "starbucks", "cafe", "restaurant", "hotel", "burger", "chai", "bakery", "dunzo"),
        "Groceries" to listOf("bigbasket", "blinkit", "grofers", "zepto", "instamart", "dmart", "jiomart",
            "more retail", "reliance fresh", "spencer", "nature basket", "kirana", "supermarket"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "tatacliq", "croma",
            "reliance digital", "decathlon", "ikea", "lifestyle", "westside", "mall"),
        "Transport" to listOf("uber", "ola", "rapido", "metro", "irctc", "redbus", "namma yatri", "fastag", "parking"),
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
            "sip", "nps", "ppf", "smallcase"),
    )

    private val PATTERNS: List<Pair<String, Regex>> = KEYWORDS.flatMap { (cat, words) ->
        words.map { w -> cat to Regex("""(?<![a-z])${Regex.escape(w)}(?![a-z])""") }
    }

    fun keywordCategory(merchant: String, direction: Direction): String {
        val m = merchant.lowercase()
        PATTERNS.firstOrNull { it.second.containsMatchIn(m) }?.let { return it.first }
        return if (direction == Direction.CREDIT) "Income" else "Other"
    }

    /** [rules] maps a lowercased merchant name to the category the user picked for it before. */
    fun categorize(merchant: String, direction: Direction, rules: Map<String, String>): String =
        categorizeWithSource(merchant, direction, rules).first

    /** Category plus where it came from: "rule" (the user's choice) or "keyword" (built-in list). */
    fun categorizeWithSource(merchant: String, direction: Direction, rules: Map<String, String>): Pair<String, String> =
        rules[merchant.lowercase()]?.let { it to CategorySource.RULE } ?: (keywordCategory(merchant, direction) to CategorySource.KEYWORD)
}
