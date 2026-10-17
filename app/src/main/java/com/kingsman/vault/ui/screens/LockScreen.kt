package com.kingsman.vault.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingsman.vault.MainActivity
import com.kingsman.vault.R
import com.kingsman.vault.VaultApp
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.ui.theme.Num
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

private const val STAGE_CHOOSE = 0
private const val STAGE_CONFIRM = 1
private const val STAGE_UNLOCK = 2

@Composable
fun LockScreen(activity: MainActivity, onUnlocked: () -> Unit) {
    val app = activity.application as VaultApp
    val security = app.security
    val prefs = app.prefs
    val c = LocalVault.current
    val scope = rememberCoroutineScope()

    var stage by remember { mutableStateOf(if (security.isPinSet()) STAGE_UNLOCK else STAGE_CHOOSE) }
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val bioAvailable = remember { activity.canUseBiometric() }
    val bioEnabled by prefs.biometric.collectAsState()
    val showBio = stage == STAGE_UNLOCK && bioEnabled && bioAvailable
    val lockoutLeft = ((prefs.lockoutUntil - now) / 1000).coerceAtLeast(0)

    fun success() {
        prefs.failedAttempts = 0
        prefs.lockoutUntil = 0
        pin = ""
        onUnlocked()
    }

    fun failed() {
        val n = prefs.failedAttempts + 1
        prefs.failedAttempts = n
        if (prefs.wipeOnFail.value && n >= 10) {
            app.wipeEverythingAndRestart()
            return
        }
        if (n >= 5) {
            prefs.lockoutUntil = System.currentTimeMillis() + 30_000L * (1L shl min(n - 5, 7))
        }
        message = buildString {
            append("Wrong PIN")
            if (prefs.wipeOnFail.value) append(" · ${10 - n} tries before all data is erased")
        }
        pin = ""
        now = System.currentTimeMillis()
    }

    fun bio() {
        if (showBio) activity.promptBiometric { success() }
    }

    LaunchedEffect(Unit) { bio() }

    LaunchedEffect(lockoutLeft > 0) {
        while (prefs.lockoutUntil > System.currentTimeMillis()) {
            delay(1000)
            now = System.currentTimeMillis()
        }
        now = System.currentTimeMillis()
    }

    fun onDigit(d: String) {
        if (busy || lockoutLeft > 0) return
        if (pin.length >= 6) return
        pin += d
        message = ""
        when (stage) {
            STAGE_UNLOCK -> if (pin.length == prefs.pinLength) {
                val attempt = pin
                busy = true
                scope.launch {
                    val ok = withContext(Dispatchers.Default) { security.verifyPin(attempt) }
                    busy = false
                    if (ok) success() else failed()
                }
            }
            STAGE_CONFIRM -> if (pin.length == firstPin.length) {
                if (pin == firstPin) {
                    val chosen = pin
                    busy = true
                    scope.launch {
                        withContext(Dispatchers.Default) { security.setPin(chosen) }
                        busy = false
                        success()
                    }
                } else {
                    message = "PINs didn't match. Try again."
                    pin = ""
                    firstPin = ""
                    stage = STAGE_CHOOSE
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Image(
            painter = painterResource(R.drawable.vault_logo),
            contentDescription = "Vault logo",
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
        )
        Spacer(Modifier.height(14.dp))
        Text("Vault", color = c.text, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            when (stage) {
                STAGE_CHOOSE -> "Create a 4–6 digit PIN"
                STAGE_CONFIRM -> "Enter the same PIN again"
                else -> "Enter your PIN"
            },
            color = c.muted, fontSize = 15.sp
        )
        Spacer(Modifier.height(28.dp))

        val dots = if (stage == STAGE_UNLOCK) prefs.pinLength else if (stage == STAGE_CONFIRM) firstPin.length else maxOf(4, pin.length)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(dots) { i ->
                val filled = i < pin.length
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (filled) c.acc else c.card)
                        .border(1.dp, if (filled) c.acc else c.line, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            when {
                lockoutLeft > 0 -> "Too many attempts. Try again in ${lockoutLeft}s"
                busy -> "Checking…"
                else -> message
            },
            color = if (message.isNotEmpty() || lockoutLeft > 0) c.exp else c.muted,
            fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.height(36.dp)
        )

        Spacer(Modifier.weight(1f))

        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("bio", "0", "del"))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { k ->
                        PinKey(
                            k = k, showBio = showBio,
                            onClick = {
                                when (k) {
                                    "bio" -> bio()
                                    "del" -> { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                                    else -> onDigit(k)
                                }
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (stage == STAGE_CHOOSE) {
            val ready = pin.length >= 4
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (ready) c.acc else c.card)
                    .clickable(enabled = ready, role = Role.Button) {
                        firstPin = pin
                        pin = ""
                        stage = STAGE_CONFIRM
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Next", color = if (ready) c.accInk else c.muted, fontWeight = FontWeight.ExtraBold)
            }
        } else {
            Spacer(Modifier.height(54.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PinKey(k: String, showBio: Boolean, onClick: () -> Unit) {
    val c = LocalVault.current
    val visible = k != "bio" || showBio
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (k.length == 1) c.card else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(enabled = visible, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            k == "bio" && showBio -> Icon(
                Icons.Rounded.Fingerprint, contentDescription = "Unlock with fingerprint",
                tint = c.accText, modifier = Modifier.size(32.dp)
            )
            k == "del" -> Text("⌫", color = c.text, fontSize = 24.sp)
            k.length == 1 -> Text(k, color = c.text, style = Num.copy(fontSize = 28.sp, fontWeight = FontWeight.Medium))
        }
    }
}
