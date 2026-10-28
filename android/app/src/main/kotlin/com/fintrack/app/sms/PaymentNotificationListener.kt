package com.fintrack.app.sms

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.fintrack.app.data.DataEvents
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
    /**
     * Connected (again): the phone may have stopped FinTrack in the background for a while. Payment
     * notifications still in the shade are read now; ones already stored are recognised and skipped.
     */
    override fun onListenerConnected() {
        try {
            activeNotifications?.forEach(::onNotificationPosted)
        } catch (_: Exception) {
            // Some phones refuse this right after connecting; new notifications still arrive.
        }
    }

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
                    val added = store.insert(parsed, category, source, Notifications.source(pkg), listOfNotNull(title, text).joinToString(". "))
                    if (added != null) DataEvents.notifyChanged()
                }
            } catch (_: Exception) {
                // Never crash the listener; a missed notification is still covered by SMS or a statement.
            }
        }.start()
    }
}
