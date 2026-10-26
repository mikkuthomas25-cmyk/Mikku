"""Keyword-based categorization. User corrections (merchant rules) win over keywords."""
from __future__ import annotations

import re

CATEGORIES = [
    "Food & Dining", "Groceries", "Shopping", "Transport", "Fuel",
    "Bills & Utilities", "Rent", "Entertainment", "Health", "Education",
    "Travel", "Cash Withdrawal", "Investments", "Transfers", "Self Transfer", "Income", "Other",
]

# Money moving between your own accounts (e.g. paying your credit card bill from
# savings). Counting it would double-count the card purchases, so totals skip it.
EXCLUDED_FROM_TOTALS = {"Self Transfer"}

KEYWORDS: dict[str, list[str]] = {
    "Self Transfer": ["credit card", "card payment", "cc payment", "cc bill", "card bill", "self transfer",
                      "own account", "cred club", "cred"],
    "Food & Dining": ["swiggy", "zomato", "eatsure", "domino", "pizza", "mcdonald", "kfc",
                      "starbucks", "cafe", "restaurant", "hotel", "burger", "chai", "bakery", "dunzo"],
    "Groceries": ["bigbasket", "blinkit", "grofers", "zepto", "instamart", "dmart", "jiomart",
                  "more retail", "reliance fresh", "spencer", "nature basket", "kirana", "supermarket"],
    "Shopping": ["amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "tatacliq", "croma",
                 "reliance digital", "decathlon", "ikea", "lifestyle", "westside", "mall"],
    "Transport": ["uber", "ola", "rapido", "metro", "irctc", "redbus", "namma yatri", "fastag", "parking"],
    "Fuel": ["petrol", "fuel", "hpcl", "bpcl", "iocl", "indian oil", "shell", "bharat petroleum"],
    "Bills & Utilities": ["electricity", "bescom", "tneb", "msedcl", "airtel", "jio", "vodafone", " vi ",
                          "bsnl", "broadband", "act fibernet", "gas", "water", "recharge", "dth",
                          "tata play", "billdesk", "insurance", "lic"],
    "Rent": ["rent", "landlord", "nobroker", "housing"],
    "Entertainment": ["netflix", "hotstar", "prime video", "spotify", "youtube", "bookmyshow",
                      "pvr", "inox", "sony liv", "zee5", "steam", "playstation"],
    "Health": ["pharmacy", "apollo", "medplus", "1mg", "pharmeasy", "netmeds", "hospital",
               "clinic", "diagnostic", "practo", "cult fit", "cultfit", "gym"],
    "Education": ["udemy", "coursera", "byju", "unacademy", "school", "college", "university", "fees"],
    "Travel": ["makemytrip", "goibibo", "cleartrip", "indigo", "air india", "vistara", "akasa",
               "spicejet", "oyo", "airbnb", "booking.com", "yatra"],
    "Cash Withdrawal": ["atm withdrawal", "atm"],
    "Investments": ["zerodha", "groww", "upstox", "indmoney", "kuvera", "coin", "mutual fund",
                    "sip", "nps", "ppf", "smallcase"],
}


def keyword_category(merchant: str, direction: str) -> str:
    m = f" {merchant.lower()} "
    for cat, words in KEYWORDS.items():
        for w in words:
            if re.search(rf"(?<![a-z]){re.escape(w.strip())}(?![a-z])", m):
                return cat
    if direction == "credit":
        return "Income"
    return "Other"


def categorize(merchant: str, direction: str, rules: dict[str, str]) -> str:
    """rules maps lowercased merchant -> category the user chose before."""
    return rules.get(merchant.lower()) or keyword_category(merchant, direction)
