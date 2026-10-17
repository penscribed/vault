package com.kingsman.vault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Vault "Midnight" palette.
 * Personal mode = brand amber, Business mode = sky blue, in both dark and light.
 */
data class VaultColors(
    val dark: Boolean,
    val bg: Color,
    val card: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val soft: Color,
    val inc: Color,
    val exp: Color,
    val bar2: Color,
    val acc: Color,
    val accInk: Color,
    val accSoft: Color,
    val accLine: Color,
    val accText: Color
)

fun vaultColors(dark: Boolean, business: Boolean): VaultColors {
    val base = if (dark) {
        listOf(0xFF0C0A08, 0xFF17130F, 0xFF2A231C, 0xFFF5EFE6, 0xFFA89C8E, 0xFFC9BFB2, 0xFF4ADE80, 0xFFF87171, 0xFF3A3129)
    } else {
        listOf(0xFFF7F5F2, 0xFFFFFFFF, 0xFFE9E3DA, 0xFF1A1410, 0xFF6B5F53, 0xFF4A4037, 0xFF15803D, 0xFFB91C1C, 0xFFE2D9CC)
    }.map { Color(it) }
    val acc = when {
        business && dark -> listOf(0xFF38BDF8, 0xFF04253A, 0xFF0A1E2B, 0xFF164E6B, 0xFF7DD3FC)
        business -> listOf(0xFF0369A1, 0xFFFFFFFF, 0xFFE0F2FE, 0xFFBAE6FD, 0xFF0369A1)
        dark -> listOf(0xFFF59E0B, 0xFF2A1500, 0xFF21180B, 0xFF4A3510, 0xFFFBBF24)
        else -> listOf(0xFFF59E0B, 0xFF2A1500, 0xFFFDF0DC, 0xFFF5D9A8, 0xFFB45309)
    }.map { Color(it) }
    return VaultColors(
        dark = dark,
        bg = base[0], card = base[1], line = base[2], text = base[3], muted = base[4], soft = base[5],
        inc = base[6], exp = base[7], bar2 = base[8],
        acc = acc[0], accInk = acc[1], accSoft = acc[2], accLine = acc[3], accText = acc[4]
    )
}

val LocalVault = staticCompositionLocalOf { vaultColors(dark = true, business = false) }
val LocalHideAmounts = staticCompositionLocalOf { false }

private val NumberFont = FontFamily.SansSerif

val Num = TextStyle(fontFamily = NumberFont, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")

@Composable
fun VaultTheme(dark: Boolean, business: Boolean, content: @Composable () -> Unit) {
    val c = vaultColors(dark, business)
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.acc, onPrimary = c.accInk,
            primaryContainer = c.accSoft, onPrimaryContainer = c.accText,
            secondary = c.acc, onSecondary = c.accInk,
            background = c.bg, onBackground = c.text,
            surface = c.card, onSurface = c.text,
            surfaceVariant = c.card, onSurfaceVariant = c.muted,
            surfaceContainer = c.card, surfaceContainerHigh = c.card,
            surfaceContainerHighest = c.card, surfaceContainerLow = c.card,
            outline = c.line, outlineVariant = c.line,
            error = c.exp
        )
    } else {
        lightColorScheme(
            primary = c.acc, onPrimary = c.accInk,
            primaryContainer = c.accSoft, onPrimaryContainer = c.accText,
            secondary = c.acc, onSecondary = c.accInk,
            background = c.bg, onBackground = c.text,
            surface = c.card, onSurface = c.text,
            surfaceVariant = c.card, onSurfaceVariant = c.muted,
            surfaceContainer = c.card, surfaceContainerHigh = c.card,
            surfaceContainerHighest = c.card, surfaceContainerLow = c.card,
            outline = c.line, outlineVariant = c.line,
            error = c.exp
        )
    }
    val type = Typography(
        bodyLarge = TextStyle(fontSize = 15.sp),
        bodyMedium = TextStyle(fontSize = 14.sp),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
    )
    CompositionLocalProvider(LocalVault provides c) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}
