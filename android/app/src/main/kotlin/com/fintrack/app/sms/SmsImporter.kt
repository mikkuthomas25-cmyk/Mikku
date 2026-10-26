package com.fintrack.app.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.fintrack.app.data.TxnStore
import com.fintrack.core.Categorizer
import com.fintrack.core.SmsParser
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object SmsImporter {
    private const val LAST_SCAN = "last_scan_millis"
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
        val defaultSince = Instant.now().minus(FIRST_SCAN_DAYS, ChronoUnit.DAYS).toEpochMilli()
        val since = if (full) defaultSince else store.getMeta(LAST_SCAN)?.toLongOrNull() ?: defaultSince
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
                if (ingest(store, body, millis, "sms", rules)) added++
            }
        }
        store.setMeta(LAST_SCAN, newest.toString())
        return added
    }

    fun ingest(store: TxnStore, body: String, millis: Long, source: String, rules: Map<String, String> = store.rules()): Boolean {
        val received = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val parsed = SmsParser.parse(body, received) ?: return false
        val category = Categorizer.categorize(parsed.merchant, parsed.direction, rules)
        return store.insert(parsed, category, source, body) != null
    }
}
