package com.kingsman.vault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kingsman.vault.ui.VaultRoot
import com.kingsman.vault.ui.VaultViewModel
import com.kingsman.vault.ui.screens.LockScreen
import com.kingsman.vault.ui.theme.LocalHideAmounts
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.ui.theme.VaultTheme
import com.kingsman.vault.ui.toast
import com.kingsman.vault.data.ROUTE_BANK
import com.kingsman.vault.util.Notifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : FragmentActivity() {

    private val vm: VaultViewModel by viewModels()
    private val app get() = application as VaultApp

    /** Screen to open after unlock (from a notification or a shared bank alert). */
    val pendingRoute = MutableStateFlow<String?>(null)

    private var permissionCallback: ((Boolean) -> Unit)? = null
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            permissionCallback?.invoke(result.values.all { it })
            permissionCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        // Budget alerts etc. while the app is open
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.events.collect { toast(it) }
            }
        }

        // Block screenshots / screen recording / recents preview (on by default)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                app.prefs.blockScreenshots.collect { secure ->
                    if (secure) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent { Content() }
    }

    @Composable
    private fun Content() {
        val theme by app.prefs.theme.collectAsState()
        val dark = when (theme) {
            "LIGHT" -> false
            "SYSTEM" -> isSystemInDarkTheme()
            else -> true
        }
        val business by vm.isBusiness.collectAsState()
        val hide by app.prefs.hideAmounts.collectAsState()
        val locked by app.lock.locked.collectAsState()

        VaultTheme(dark = dark, business = business) {
            val c = LocalVault.current
            val view = LocalView.current
            SideEffect {
                val ctl = WindowCompat.getInsetsController(window, view)
                ctl.isAppearanceLightStatusBars = !dark
                ctl.isAppearanceLightNavigationBars = !dark
            }
            CompositionLocalProvider(LocalHideAmounts provides hide) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(c.bg)
                        .safeDrawingPadding()
                ) {
                    if (app.security.isPinSet()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .alpha(if (locked) 0f else 1f)
                                .then(if (locked) Modifier.clearAndSetSemantics { } else Modifier)
                        ) {
                            VaultRoot(vm, this@MainActivity)
                        }
                    }
                    if (locked) {
                        LockScreen(this@MainActivity) {
                            app.lock.locked.value = false
                            vm.onUnlocked()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        intent.getStringExtra(Notifier.EXTRA_OPEN)?.let { pendingRoute.value = it }
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                vm.addPastedAlert(text) { ok ->
                    toast(if (ok) "Bank alert added — review it in Bank alerts" else "That doesn't look like a bank alert")
                    if (ok) pendingRoute.value = ROUTE_BANK
                }
            }
        }
        setIntent(Intent())
    }

    // ---------------- Permissions ----------------
    fun hasPermission(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    fun askPermissions(perms: List<String>, onResult: (Boolean) -> Unit) {
        val missing = perms.filterNot { hasPermission(it) }
        if (missing.isEmpty()) { onResult(true); return }
        permissionCallback = onResult
        prepareExternal()
        permissionLauncher.launch(missing.toTypedArray())
    }

    /** Asks for notification permission on Android 13+ (no-op elsewhere). */
    fun ensureNotificationPermission(onResult: (Boolean) -> Unit = {}) {
        if (Build.VERSION.SDK_INT >= 33) askPermissions(listOf(Manifest.permission.POST_NOTIFICATIONS), onResult)
        else onResult(true)
    }

    // ---------------- Auto-lock ----------------
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) app.lock.backgroundAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        val lock = app.lock
        if (lock.skipNextLock) {
            lock.skipNextLock = false
            return
        }
        if (lock.backgroundAt > 0) {
            val away = SystemClock.elapsedRealtime() - lock.backgroundAt
            if (away >= app.prefs.autoLockSec.value * 1000L) lock.locked.value = true
        }
    }

    fun lockNow() {
        app.lock.locked.value = true
    }

    /** Call before launching another app (share sheet, file picker) so coming back doesn't re-lock. */
    fun prepareExternal() {
        app.lock.skipNextLock = true
    }

    // ---------------- Biometrics ----------------
    fun canUseBiometric(): Boolean =
        BiometricManager.from(this).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    fun promptBiometric(onSuccess: () -> Unit) {
        if (!canUseBiometric()) return
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Vault")
            .setSubtitle("Confirm it's you")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    // ---------------- Sharing files ----------------
    fun shareFile(file: File, mime: String, subject: String, text: String) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        prepareExternal()
        startActivity(Intent.createChooser(send, subject))
    }

    fun openFile(file: File, mime: String) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        prepareExternal()
        runCatching { startActivity(Intent.createChooser(view, "Open report")) }
    }
}
