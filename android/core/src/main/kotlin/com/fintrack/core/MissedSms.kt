package com.fintrack.core

/** A message that looks like a bank transaction but the parser couldn't read. */
data class MissedSms(val sender: String, val millis: Long, val body: String)
