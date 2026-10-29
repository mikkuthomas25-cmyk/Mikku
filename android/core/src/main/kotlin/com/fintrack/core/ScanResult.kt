package com.fintrack.core

/**
 * What a read of the SMS inbox saw, so "nothing changed" can be told apart: the phone showed no
 * bank SMS, they were all known already, or their wording couldn't be read.
 */
data class ScanResult(
    val messages: Int = 0,
    val payments: Int = 0,
    val added: Int = 0,
    val unreadable: Int = 0,
    /** Payments inside dates an imported statement covers: the statement is used there instead. */
    val coveredByStatement: Int = 0,
) {
    fun summary(full: Boolean): String = when {
        messages == 0 -> "The phone showed FinTrack no messages${if (full) " from the last 6 months" else ""}. Check that SMS access is allowed."
        payments == 0 && unreadable == 0 -> "Read $messages messages, but none were bank payments. If your bank SMS are in another app or folder, FinTrack can't see them."
        else -> buildString {
            append("Read $messages messages: $payments payment${if (payments == 1) "" else "s"}")
            append(if (added == 0) ", all already in FinTrack" else " ($added new)")
            if (coveredByStatement > 0) append(". $coveredByStatement fall in dates your statement covers, so the statement is used there")
            if (unreadable > 0) append(". $unreadable bank message${if (unreadable == 1) "" else "s"} couldn't be read: see Settings → Missed messages")
            append(".")
        }
    }
}
