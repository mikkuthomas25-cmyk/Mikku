package com.fintrack.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.fintrack.app.data.DataEvents
import com.fintrack.app.data.Prefs
import com.fintrack.app.data.TxnStore

/**
 * Captures transaction SMS the moment they arrive, even when the app is closed.
 * Anything missed here is picked up by the inbox scan the next time the app opens.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val dbName = Prefs(context).activeDb ?: return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return
        // Long SMS arrive as several parts; join them back into one message.
        val body = parts.joinToString("") { it.messageBody ?: "" }
        val millis = parts.first().timestampMillis

        val pending = goAsync()
        Thread {
            try {
                TxnStore(context.applicationContext, dbName).use { store ->
                    if (SmsImporter.ingest(store, body, millis, "sms")) DataEvents.notifyChanged()
                }
            } catch (_: Exception) {
                // Never crash the system broadcast; the next inbox scan retries.
            } finally {
                pending.finish()
            }
        }.start()
    }
}
