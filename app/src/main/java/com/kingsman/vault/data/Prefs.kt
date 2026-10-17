package com.kingsman.vault.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App settings. Nothing secret is stored here in plain form:
 * the PIN is stored only as a salted PBKDF2 hash, and the database key is
 * wrapped by a key that never leaves the Android Keystore.
 */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)

    private fun str(key: String, def: String) = MutableStateFlow(sp.getString(key, def) ?: def)
    private fun bool(key: String, def: Boolean) = MutableStateFlow(sp.getBoolean(key, def))

    // ----- Observable settings -----
    private val _theme = str("theme", "DARK")                 // DARK, LIGHT, SYSTEM
    private val _mode = str("mode", Scope.PERSONAL)          // PERSONAL, BUSINESS
    private val _businessId = MutableStateFlow(sp.getLong("businessId", -1L))
    private val _currency = str("currency", "NGN")
    private val _hide = bool("hideAmounts", false)
    private val _biometric = bool("biometric", false)
    private val _autoLock = MutableStateFlow(sp.getInt("autoLockSec", 30))
    private val _blockShots = bool("blockScreenshots", true)
    private val _wipeOnFail = bool("wipeOnFail", false)
    private val _lastBackup = MutableStateFlow(sp.getLong("lastBackup", 0L))
    private val _sms = bool("smsEnabled", false)
    private val _autoUpdate = bool("autoUpdate", true)
    private val _availableUpdate = str("availableUpdate", "")

    val theme: StateFlow<String> = _theme
    val mode: StateFlow<String> = _mode
    val businessId: StateFlow<Long> = _businessId
    val defaultCurrency: StateFlow<String> = _currency
    val hideAmounts: StateFlow<Boolean> = _hide
    val biometric: StateFlow<Boolean> = _biometric
    val autoLockSec: StateFlow<Int> = _autoLock
    val blockScreenshots: StateFlow<Boolean> = _blockShots
    val wipeOnFail: StateFlow<Boolean> = _wipeOnFail
    val lastBackup: StateFlow<Long> = _lastBackup
    val smsEnabled: StateFlow<Boolean> = _sms
    val autoUpdate: StateFlow<Boolean> = _autoUpdate
    /** Version found on GitHub that is newer than this app ("" = none). */
    val availableUpdate: StateFlow<String> = _availableUpdate

    fun setTheme(v: String) { sp.edit().putString("theme", v).apply(); _theme.value = v }
    fun setMode(v: String) { sp.edit().putString("mode", v).apply(); _mode.value = v }
    fun setBusinessId(v: Long) { sp.edit().putLong("businessId", v).apply(); _businessId.value = v }
    fun setDefaultCurrency(v: String) { sp.edit().putString("currency", v).apply(); _currency.value = v }
    fun setHideAmounts(v: Boolean) { sp.edit().putBoolean("hideAmounts", v).apply(); _hide.value = v }
    fun setBiometric(v: Boolean) { sp.edit().putBoolean("biometric", v).apply(); _biometric.value = v }
    fun setAutoLockSec(v: Int) { sp.edit().putInt("autoLockSec", v).apply(); _autoLock.value = v }
    fun setBlockScreenshots(v: Boolean) { sp.edit().putBoolean("blockScreenshots", v).apply(); _blockShots.value = v }
    fun setWipeOnFail(v: Boolean) { sp.edit().putBoolean("wipeOnFail", v).apply(); _wipeOnFail.value = v }
    fun setLastBackup(v: Long) { sp.edit().putLong("lastBackup", v).apply(); _lastBackup.value = v }
    fun setSmsEnabled(v: Boolean) { sp.edit().putBoolean("smsEnabled", v).apply(); _sms.value = v }
    fun setAutoUpdate(v: Boolean) { sp.edit().putBoolean("autoUpdate", v).apply(); _autoUpdate.value = v }
    fun setAvailableUpdate(v: String) { sp.edit().putString("availableUpdate", v).apply(); _availableUpdate.value = v }

    // ----- Security material (not observable) -----
    var pinHash: String?
        get() = sp.getString("pinHash", null)
        set(v) { sp.edit().putString("pinHash", v).commit() }
    var pinSalt: String?
        get() = sp.getString("pinSalt", null)
        set(v) { sp.edit().putString("pinSalt", v).commit() }
    var pinLength: Int
        get() = sp.getInt("pinLength", 4)
        set(v) { sp.edit().putInt("pinLength", v).commit() }
    var failedAttempts: Int
        get() = sp.getInt("failedAttempts", 0)
        set(v) { sp.edit().putInt("failedAttempts", v).commit() }
    var lockoutUntil: Long
        get() = sp.getLong("lockoutUntil", 0L)
        set(v) { sp.edit().putLong("lockoutUntil", v).commit() }
    var dbKeyWrapped: String?
        get() = sp.getString("dbKey", null)
        set(v) { sp.edit().putString("dbKey", v).commit() }
    var dbKeyIv: String?
        get() = sp.getString("dbKeyIv", null)
        set(v) { sp.edit().putString("dbKeyIv", v).commit() }
    var lastSmsScan: Long
        get() = sp.getLong("lastSmsScan", 0L)
        set(v) { sp.edit().putLong("lastSmsScan", v).apply() }
    var lastUpdateCheck: Long
        get() = sp.getLong("lastUpdateCheck", 0L)
        set(v) { sp.edit().putLong("lastUpdateCheck", v).apply() }
    var notifiedVersion: String
        get() = sp.getString("notifiedVersion", "") ?: ""
        set(v) { sp.edit().putString("notifiedVersion", v).apply() }
    var seeded: Boolean
        get() = sp.getBoolean("seeded", false)
        set(v) { sp.edit().putBoolean("seeded", v).commit() }

    fun clearAll() { sp.edit().clear().commit() }
}
