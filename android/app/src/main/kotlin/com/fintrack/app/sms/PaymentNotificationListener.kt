package com.fintrack.app.sms

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.fintrack.app.data.Prefs
import com.fintrack.app.data.TxnStore
import com.fintrack.core.Categorizer
import com.fintrack.core.Notifications
import java.time.Instant
import java.time.ZoneId

/**
 * Reads payment notifications from the apps in [Notifications.PAYMENT_APPS] (Google Pay, PhonePe,
 * Paytm, bank apps…) and Truecaller, once the user turns on Notification access for FinTrack.
 * Every other app's notifications are ignored without being read.
 */
class PaymentNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (!Notifications.isWatched(pkg)) return
        val dbName = Prefs(this).activeDb ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        val date = Instant.ofEpochMilli(sbn.postTime).atZone(ZoneId.systemDefault()).toLocalDate()
        val parsed = Notifications.parse(pkg, sbn.key, title, text, date) ?: return
        Thread {
            try {
                TxnStore(applicationContext, dbName).use { store ->
                    val (category, source) = Categorizer.categorizeWithSource(parsed.merchant, parsed.direction, store.rules())
                    store.insert(parsed, category, source, Notifications.source(pkg), listOfNotNull(title, text).joinToString(". "))
                }
            } catch (_: Exception) {
                // Never crash the listener; a missed notification is still covered by SMS or a statement.
            }
        }.start()
    }
}
