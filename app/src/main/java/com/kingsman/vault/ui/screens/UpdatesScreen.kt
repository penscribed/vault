package com.kingsman.vault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingsman.vault.BuildConfig
import com.kingsman.vault.MainActivity
import com.kingsman.vault.ui.PrimaryButton
import com.kingsman.vault.ui.ProgressLine
import com.kingsman.vault.ui.ScreenHeader
import com.kingsman.vault.ui.SecondaryButton
import com.kingsman.vault.ui.SettingSwitch
import com.kingsman.vault.ui.VCard
import com.kingsman.vault.ui.VaultViewModel
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.update.ReleaseInfo
import com.kingsman.vault.update.Updater
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdatesScreen(vm: VaultViewModel, activity: MainActivity, onBack: () -> Unit) {
    val c = LocalVault.current
    val scope = rememberCoroutineScope()
    val auto by vm.prefs.autoUpdate.collectAsState()

    var checking by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(false) }
    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(-1f) }
    var apk by remember { mutableStateOf<File?>(null) }

    fun check() {
        if (checking) return
        checking = true
        error = null
        scope.launch {
            runCatching { Updater.checkForUpdate() }
                .onSuccess {
                    release = it
                    checked = true
                    vm.prefs.lastUpdateCheck = System.currentTimeMillis()
                    vm.prefs.setAvailableUpdate(it?.version ?: "")
                }
                .onFailure { error = "Couldn't reach GitHub. Check your internet connection and try again." }
            checking = false
        }
    }

    fun install(file: File) {
        if (!Updater.canInstall(activity)) {
            activity.prepareExternal()
            activity.startActivity(Updater.permissionIntent(activity))
            return
        }
        activity.prepareExternal()
        activity.startActivity(Updater.installIntent(activity, file))
    }

    fun download(r: ReleaseInfo) {
        if (progress >= 0f) return
        progress = 0f
        error = null
        scope.launch {
            runCatching { Updater.download(activity, r) { p -> progress = p } }
                .onSuccess { file ->
                    apk = file
                    install(file)
                }
                .onFailure { error = it.message ?: "Download failed" }
            progress = -1f
        }
    }

    LaunchedEffect(Unit) { check() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("App updates", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VCard(Modifier.fillMaxWidth()) {
                Text("Vault ${BuildConfig.VERSION_NAME}", color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(
                    when {
                        checking -> "Checking GitHub for updates…"
                        release != null -> "A new version is ready"
                        checked -> "You're on the latest version"
                        else -> "Updates come from github.com/${BuildConfig.UPDATE_REPO}"
                    },
                    color = c.muted, fontSize = 13.sp
                )
            }

            val r = release
            if (r != null) {
                VCard(Modifier.fillMaxWidth(), bg = c.accSoft, border = c.accLine) {
                    Text("Vault ${r.version}", color = c.text, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    if (r.apkSize > 0) {
                        Text("${"%.1f".format(r.apkSize / 1_048_576.0)} MB", color = c.soft, fontSize = 12.sp)
                    }
                    if (r.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(r.notes.take(1500), color = c.soft, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (progress >= 0f) {
                        Text("Downloading… ${(progress * 100).toInt()}%", color = c.text, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        ProgressLine(progress)
                    } else {
                        val file = apk
                        PrimaryButton(
                            if (file != null) "Install update" else "Download & install",
                            Modifier.fillMaxWidth()
                        ) { if (file != null) install(file) else download(r) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your records, PIN and settings stay exactly as they are. Android will ask you to confirm the install.",
                        color = c.muted, fontSize = 12.sp
                    )
                }
                if (!Updater.canInstall(activity)) {
                    Text(
                        "The first time, Android asks you to allow installs from Vault. Switch it on, come back and tap Install.",
                        color = c.muted, fontSize = 12.sp
                    )
                }
            }

            error?.let { Text(it, color = c.exp, fontSize = 13.sp) }

            SecondaryButton(if (checking) "Checking…" else "Check now", Modifier.fillMaxWidth()) { check() }

            VCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                SettingSwitch(
                    "Check automatically",
                    "Once a day when you're online, plus when you open Vault",
                    auto
                ) { vm.setAutoUpdate(it) }
                Text(
                    "Update checks only contact GitHub. Nothing about you or your money is sent. " +
                        "Each download is verified against its published checksum, and Android only accepts it " +
                        "if it's signed with the same key as the Vault you have installed.",
                    color = c.muted, fontSize = 12.sp
                )
            }
        }
    }
}
