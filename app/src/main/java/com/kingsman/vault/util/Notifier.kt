package com.kingsman.vault.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kingsman.vault.MainActivity
import com.kingsman.vault.R

object Notifier {
    const val CH_BUDGET = "budgets"
    const val CH_BANK = "bank_alerts"
    const val CH_UPDATE = "updates"
    const val EXTRA_OPEN = "open"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_BUDGET, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "When a category gets close to or over its monthly budget"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_BANK, "Bank alerts to review", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "When a bank SMS can be added as an entry"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_UPDATE, "App updates", NotificationManager.IMPORTANCE_LOW).apply {
                description = "When a new version of Vault is available"
            }
        )
    }

    fun canNotify(ctx: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    /**
     * Shows a notification whose details are hidden on the lock screen
     * (only [publicTitle] is visible until the phone is unlocked).
     */
    @SuppressLint("MissingPermission")
    fun show(ctx: Context, channel: String, id: Int, title: String, text: String, publicTitle: String, openRoute: String) {
        if (!canNotify(ctx)) return
        ensureChannels(ctx)
        val intent = Intent(ctx, MainActivity::class.java)
            .putExtra(EXTRA_OPEN, openRoute)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            ctx, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val public = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_vault)
            .setContentTitle(publicTitle)
            .setContentText("Open Vault to see details")
            .build()
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_vault)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(id, n) }
    }
}
