package com.kingsman.vault

import android.app.Application
import android.content.Intent
import androidx.room.Room
import com.kingsman.vault.data.Prefs
import com.kingsman.vault.data.Repository
import com.kingsman.vault.data.VaultDatabase
import com.kingsman.vault.security.SecurityManager
import com.kingsman.vault.update.UpdateWorker
import com.kingsman.vault.util.Notifier
import com.kingsman.vault.util.ReceiptStore
import kotlinx.coroutines.flow.MutableStateFlow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class LockState {
    /** True whenever the app content must be hidden behind the PIN screen. */
    val locked = MutableStateFlow(true)
    var backgroundAt = 0L
    /** Set right before opening the share sheet / file picker so returning doesn't re-lock. */
    var skipNextLock = false
}

class VaultApp : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var security: SecurityManager
        private set
    val lock = LockState()

    private var database: VaultDatabase? = null

    val db: VaultDatabase
        get() = database ?: synchronized(this) {
            database ?: buildDb().also { database = it }
        }

    val receipts: ReceiptStore by lazy { ReceiptStore(this, security) }

    val repo: Repository by lazy { Repository(this, db, prefs, security, receipts) }

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        prefs = Prefs(this)
        security = SecurityManager(prefs)
        Notifier.ensureChannels(this)
        if (prefs.autoUpdate.value) runCatching { UpdateWorker.schedule(this) }
    }

    private fun buildDb(): VaultDatabase {
        val key = security.databasePassphrase()
        return Room.databaseBuilder(this, VaultDatabase::class.java, DB_NAME)
            .openHelperFactory(SupportOpenHelperFactory(key))
            .addMigrations(VaultDatabase.MIGRATION_1_2)
            .build()
    }

    /** Erases every trace of the user's data and restarts the app fresh. */
    fun wipeEverythingAndRestart() {
        runCatching { database?.close() }
        database = null
        deleteDatabase(DB_NAME)
        prefs.clearAll()
        security.deleteKeys()
        cacheDir.deleteRecursively()
        java.io.File(filesDir, "receipts").deleteRecursively()
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (intent != null) startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    companion object {
        const val DB_NAME = "vault.db"
    }
}
