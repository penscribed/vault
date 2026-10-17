package com.kingsman.vault.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

data class CurrencyItem(val code: String, val name: String)

object Money {
    private val symbols = mapOf(
        "NGN" to "₦", "USD" to "$", "GBP" to "£", "EUR" to "€", "GHS" to "GH₵",
        "KES" to "KSh ", "ZAR" to "R", "INR" to "₹", "JPY" to "¥", "CNY" to "CN¥",
        "CAD" to "CA$", "AUD" to "A$", "XOF" to "CFA ", "XAF" to "FCFA ", "EGP" to "E£",
        "AED" to "AED ", "SAR" to "SAR ", "CHF" to "CHF ", "UGX" to "USh ", "TZS" to "TSh ",
        "RWF" to "RF ", "BRL" to "R$", "MXN" to "MX$", "KRW" to "₩", "TRY" to "₺", "PHP" to "₱"
    )

    fun symbol(code: String): String =
        symbols[code] ?: runCatching { Currency.getInstance(code).getSymbol(Locale.getDefault()) }
            .getOrNull()
            ?.let { if (it == code) "$code " else it }
        ?: "$code "

    private val full = DecimalFormat("#,##0.00")

    /** 4850000 -> "₦48,500.00" */
    fun format(minor: Long, code: String, signed: Boolean = false): String {
        val sign = when {
            signed && minor > 0 -> "+"
            minor < 0 -> "−"
            else -> ""
        }
        return sign + symbol(code) + full.format(abs(minor) / 100.0)
    }

    /** 185000000 -> "₦1.85M" */
    fun compact(minor: Long, code: String, signed: Boolean = false): String {
        val v = abs(minor) / 100.0
        val body = when {
            v >= 1_000_000_000 -> trim(v / 1_000_000_000) + "B"
            v >= 1_000_000 -> trim(v / 1_000_000) + "M"
            v >= 10_000 -> trim(v / 1_000) + "k"
            else -> DecimalFormat("#,##0.##").format(v)
        }
        val sign = when {
            signed && minor > 0 -> "+"
            minor < 0 -> "−"
            else -> ""
        }
        return sign + symbol(code) + body
    }

    private fun trim(d: Double): String = DecimalFormat("0.##").format(d)

    /** "48500.5" -> 4850050. Returns null for invalid input. */
    fun parse(text: String): Long? = runCatching {
        val clean = text.replace(",", "").trim()
        if (clean.isEmpty()) return null
        BigDecimal(clean).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrNull()

    /** 4850050 -> "48500.5" (for editing). */
    fun toInput(minor: Long): String =
        BigDecimal(minor).divide(BigDecimal(100)).stripTrailingZeros().toPlainString()

    val allCurrencies: List<CurrencyItem> by lazy {
        Currency.getAvailableCurrencies()
            .map { CurrencyItem(it.currencyCode, it.getDisplayName(Locale.getDefault())) }
            .sortedBy { it.code }
    }

    fun name(code: String): String =
        runCatching { Currency.getInstance(code).getDisplayName(Locale.getDefault()) }.getOrDefault(code)
}
