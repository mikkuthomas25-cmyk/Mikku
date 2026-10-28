package com.fintrack.core

import java.time.LocalDate

/**
 * Payments from app notifications (Google Pay, PhonePe, Paytm, bank apps…), which often arrive when
 * no SMS does. Only the apps listed here are read; a chat app's "I paid ₹500" must never become a
 * transaction.
 */
object Notifications {
    /** Payment and bank apps, by package name. */
    val PAYMENT_APPS: Map<String, String> = mapOf(
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.phonepe.app" to "PhonePe",
        "net.one97.paytm" to "Paytm",
        "in.org.npci.upiapp" to "BHIM",
        "com.dreamplug.androidapp" to "CRED",
        "in.amazon.mShop.android.shopping" to "Amazon Pay",
        "indwin.c3.shareapp" to "slice",
        "com.sliceit.app" to "slice",
        "com.mobikwik_new" to "MobiKwik",
        "com.freecharge.android" to "Freecharge",
        "com.sbi.lotusintouch" to "YONO SBI",
        "com.sbi.SBIFreedomPlus" to "YONO Lite SBI",
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.csam.icici.bank.imobile" to "ICICI iMobile",
        "com.axis.mobile" to "Axis Mobile",
        "com.msf.kbank.mobile" to "Kotak",
        "com.iob.iobmobile" to "IOB",
        "com.canarabank.mobility" to "Canara ai1",
        "com.fedmobile" to "FedMobile",
        "com.bankofbaroda.mconnect" to "bob World",
        "com.idfcfirstbank.optimus" to "IDFC First",
        "com.jupiter.money" to "Jupiter",
        "com.epifi.paisa" to "Fi",
    )

    /** Apps that show SMS as notifications: their text is an SMS, matched to the SMS itself. */
    val SMS_APPS: Map<String, String> = mapOf(
        "com.truecaller" to "Truecaller",
    )

    fun isWatched(pkg: String) = pkg in PAYMENT_APPS || pkg in SMS_APPS

    /**
     * A notification as a transaction, or null. [key] identifies the notification (same key = the
     * same notification updated, not a new payment). Text from an SMS app is hashed like the SMS,
     * so the SMS and its notification are stored once.
     */
    fun parse(pkg: String, key: String, title: String?, text: String?, date: LocalDate): ParsedTxn? {
        if (!isWatched(pkg)) return null
        val body = listOfNotNull(title?.trim(), text?.trim()).filter { it.isNotEmpty() }.joinToString(". ")
        if (body.isBlank() || PROMO.containsMatchIn(body)) return null
        val parsed = SmsParser.parse(body, date) ?: return null
        return if (pkg in SMS_APPS) parsed
        else parsed.copy(smsHash = SmsParser.hash("$pkg|$key|$body"))
    }

    /** Source label stored with the transaction, e.g. "notification:Google Pay". */
    fun source(pkg: String) = "notification:" + (PAYMENT_APPS[pkg] ?: SMS_APPS[pkg] ?: pkg)

    fun isNotification(source: String) = source.startsWith("notification:")

    // Offers, rewards and reminders that mention money but aren't a payment.
    private val PROMO = Regex(
        """\b(cashback\s+(?:up\s*to|offer)|scratch\s*card|reward|you\s+(?:have\s+)?won|get\s+(?:up\s*to\s+)?₹|pay\s+now|bill\s+(?:is\s+)?due|""" +
            """reminder|expires?|limited\s+time|apply\s+now|pre-?approved|loan\s+offer|request(?:ed)?\s+(?:₹|rs|inr|money))\b""",
        RegexOption.IGNORE_CASE,
    )
}
