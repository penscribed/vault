package com.kingsman.vault

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Listens for incoming SMS (only when the user switched "Read bank SMS" on).
 * Bank alerts become suggestions + a notification; every other message is ignored and never stored.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val app = context.applicationContext as VaultApp
        if (!app.prefs.smsEnabled.value) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Long SMS arrive in parts: join them per sender.
        val bySender = messages.groupBy { it.originatingAddress ?: "" }
            .mapValues { (_, parts) -> parts.joinToString("") { it.messageBody ?: "" } to (parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()) }
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                bySender.forEach { (sender, pair) ->
                    runCatching { app.repo.ingestAlert(sender, pair.first, pair.second, notify = true) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
