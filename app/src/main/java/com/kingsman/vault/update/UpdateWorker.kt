package com.kingsman.vault.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kingsman.vault.VaultApp
import com.kingsman.vault.util.Notifier
import java.util.concurrent.TimeUnit

const val ROUTE_UPDATES = "updates"

/** Checks GitHub once a day (when online) and notifies when a newer Vault is published. */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as VaultApp
        if (!app.prefs.autoUpdate.value) return Result.success()
        val release = runCatching { Updater.checkForUpdate() }.getOrElse { return Result.retry() }
        app.prefs.lastUpdateCheck = System.currentTimeMillis()
        if (release == null) {
            app.prefs.setAvailableUpdate("")
        } else {
            app.prefs.setAvailableUpdate(release.version)
            if (app.prefs.notifiedVersion != release.version) {
                Notifier.show(
                    app, Notifier.CH_UPDATE, 3000,
                    "Vault ${release.version} is available",
                    "Tap to update. Your records stay exactly as they are.",
                    "Vault update available",
                    ROUTE_UPDATES
                )
                app.prefs.notifiedVersion = release.version
            }
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "vault-update-check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
