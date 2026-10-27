package com.fintrack.app.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.fintrack.app.data.TxnStore
import com.fintrack.core.Categorizer
import com.fintrack.core.MissedSms
import com.fintrack.core.SmsParser
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object SmsImporter {
    private const val LAST_SCAN = "last_scan_millis"
    private const val PARSER_VERSION = "parser_version"
    private const val FIRST_SCAN_DAYS = 180L

    fun hasPermission(context: Context) = listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        .all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /**
     * Reads inbox messages newer than the last scan (the last 6 months on first run, or
     * when [full] is set) and stores the transactions. Returns how many were added.
     * Safe to call repeatedly: duplicates are ignored.
     */
    fun scanInbox(context: Context, store: TxnStore, full: Boolean = false): Int {
        if (!hasPermission(context)) return 0
        // A newer parser can read messages the old one skipped: re-read history once.
        val reparse = store.getMeta(PARSER_VERSION) != SmsParser.VERSION.toString()
        val defaultSince = Instant.now().minus(FIRST_SCAN_DAYS, ChronoUnit.DAYS).toEpochMilli()
        val since = if (full || reparse) defaultSince else store.getMeta(LAST_SCAN)?.toLongOrNull() ?: defaultSince
        val rules = store.rules()
        var added = 0
        var newest = since
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(since.toString()),
            "${Telephony.Sms.DATE} ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val body = c.getString(0) ?: continue
                val millis = c.getLong(1)
                newest = maxOf(newest, millis)
                if (ingest(store, body, millis, "sms", rules)) {
                    added++
                } else if (reparse) {
                    // Already stored: pick up a name the old parser got wrong ("Rs", "Unknown").
                    SmsParser.parse(body)?.let { store.repairMerchant(it.smsHash, it.merchant) }
                }
            }
        }
        store.setMeta(LAST_SCAN, newest.toString())
        store.setMeta(PARSER_VERSION, SmsParser.VERSION.toString())
        return added
    }

    /**
     * Bank-looking messages from the last [days] that the parser couldn't turn into a
     * transaction, newest first. Shown in Profile so formats we miss can be reported and fixed.
     */
    fun findMissed(context: Context, days: Long = FIRST_SCAN_DAYS, limit: Int = 200): List<MissedSms> {
        if (!hasPermission(context)) return emptyList()
        val since = Instant.now().minus(days, ChronoUnit.DAYS).toEpochMilli()
        val out = mutableListOf<MissedSms>()
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(since.toString()),
            "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                val body = c.getString(2) ?: continue
                if (SmsParser.looksFinancial(body) && SmsParser.parse(body) == null) {
                    out += MissedSms(c.getString(0) ?: "", c.getLong(1), body)
                }
            }
        }
        return out
    }

    fun ingest(store: TxnStore, body: String, millis: Long, source: String, rules: Map<String, String> = store.rules()): Boolean {
        val received = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val parsed = SmsParser.parse(body, received) ?: return false
        val (category, catSource) = Categorizer.categorizeWithSource(parsed.merchant, parsed.direction, rules)
        return store.insert(parsed, category, catSource, source, body) != null
    }
}
