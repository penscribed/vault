package com.kingsman.vault.util

import com.kingsman.vault.data.Kind
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

data class ParsedAlert(
    val kind: String,
    val amount: Long,
    val currency: String,
    val description: String
)

/**
 * Reads debit / credit alerts from banks and wallets (GTBank, Access, Zenith, UBA, First Bank,
 * Opay, Kuda, Moniepoint, Palmpay, US/UK banks…) without needing any bank-specific templates.
 * Anything that doesn't look like a bank alert returns null and is never stored.
 */
object BankSmsParser {

    private val currencyAmount = Regex(
        """(?<![A-Za-z])(NGN|USD|GBP|EUR|GHS|KES|ZAR|CAD|US\$|N|₦|\$|£|€)\s?:?\s?(\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)"""
    )
    private val labelledAmount = Regex(
        """(?i)\b(?:amt|amount|value)\s*[:=]?\s*(?:NGN|N|₦)?\s?(\d{1,3}(?:,\d{3})+(?:\.\d{1,2})?|\d+(?:\.\d{1,2})?)"""
    )
    private val debitWords = Regex(
        """(?i)\b(debit|debited|dr|withdrawal|withdrawn|purchase|pos|spent|sent|transfer to|paid|payment|charged|atm|made a|transaction with)\b"""
    )
    private val creditWords = Regex(
        """(?i)\b(credit|credited|cr|received|deposit|deposited|inflow|reversal|refund|transfer from)\b"""
    )
    private val bankMarkers = Regex(
        """(?i)(acct|a/c|account|card|alert|txn|trans|\*\d{3,4}|\d{3}\*+\d{2,4}|x{2,}\d{3,4}|bal|wallet)"""
    )
    private val balanceBefore = Regex("""(?i)(bal|balance|avail|avl|ledger|ldg)[^0-9]{0,14}$""")
    private val descRx = Regex(
        """(?i)\b(?:desc|des|description|narration|narr|details|remark|rmk)\s*[:\-]\s*(.+?)(?=\s*(?:\b(?:avail|avl|bal|balance|date|time|dt)\b|\n|$))"""
    )
    private val atRx = Regex("""(?i)\b(?:at|to|from)\s+([A-Za-z0-9][A-Za-z0-9 .&'\-]{2,40})""")

    fun parse(body: String, sender: String, defaultCurrency: String): ParsedAlert? {
        val text = body.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        if (text.length < 12 || !bankMarkers.containsMatchIn(text)) return null

        // Debit or credit? Explicit DR/CR words win; otherwise whichever appears first.
        val explicit = Regex("""(?i)\b(DR|CR|Debit|Credit)\b""").find(text)?.value?.lowercase()
        val kind = when {
            explicit == "dr" || explicit == "debit" -> Kind.EXPENSE
            explicit == "cr" || explicit == "credit" -> Kind.INCOME
            else -> {
                val d = debitWords.find(text)?.range?.first
                val c = creditWords.find(text)?.range?.first
                when {
                    d == null && c == null -> return null
                    c == null -> Kind.EXPENSE
                    d == null -> Kind.INCOME
                    d < c -> Kind.EXPENSE
                    else -> Kind.INCOME
                }
            }
        }

        // First amount that isn't a balance.
        var currency = defaultCurrency
        var amountText: String? = null
        for (m in currencyAmount.findAll(text)) {
            val before = text.substring(maxOf(0, m.range.first - 16), m.range.first)
            if (balanceBefore.containsMatchIn(before)) continue
            currency = mapCurrency(m.groupValues[1], defaultCurrency)
            amountText = m.groupValues[2]
            break
        }
        if (amountText == null) {
            val m = labelledAmount.find(text) ?: return null
            amountText = m.groupValues[1]
        }
        val amt: String = amountText ?: return null
        val minor = runCatching {
            BigDecimal(amt.replace(",", "")).multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP).longValueExact()
        }.getOrNull() ?: return null
        if (minor <= 0) return null

        val desc = descRx.find(text)?.groupValues?.get(1)?.trim()?.trimEnd('.', ',', ';')
            ?: atRx.find(text)?.groupValues?.get(1)?.trim()
            ?: sender
        return ParsedAlert(kind, minor, currency, desc.take(60))
    }

    private fun mapCurrency(token: String, default: String): String = when (token.uppercase()) {
        "N", "₦", "NGN" -> "NGN"
        "$", "US$", "USD" -> "USD"
        "£", "GBP" -> "GBP"
        "€", "EUR" -> "EUR"
        "GHS", "KES", "ZAR", "CAD" -> token.uppercase()
        else -> default
    }

    fun hash(sender: String, body: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((sender.trim() + "|" + body.trim()).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
