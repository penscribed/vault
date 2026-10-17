package com.kingsman.vault.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kingsman.vault.ui.theme.LocalHideAmounts
import com.kingsman.vault.ui.theme.LocalVault
import com.kingsman.vault.ui.theme.Num
import com.kingsman.vault.util.Dates
import com.kingsman.vault.util.Money
import java.time.LocalDate

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

/** Money text that respects the "hide amounts" eye toggle. */
@Composable
fun AmountText(
    minor: Long,
    currency: String,
    modifier: Modifier = Modifier,
    size: Int = 15,
    color: Color = LocalVault.current.text,
    compact: Boolean = false,
    signed: Boolean = false,
    prefix: String = "",
    weight: FontWeight = FontWeight.Bold
) {
    val hide = LocalHideAmounts.current
    val s = when {
        hide -> "••••••"
        compact -> Money.compact(minor, currency, signed)
        else -> Money.format(minor, currency, signed)
    }
    Text(
        prefix + s, modifier = modifier, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis,
        style = Num.copy(fontSize = size.sp, fontWeight = weight)
    )
}

@Composable
fun VCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    bg: Color = LocalVault.current.card,
    border: Color = LocalVault.current.line,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun VChip(
    text: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
    onClick: () -> Unit
) {
    val c = LocalVault.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(if (selected) c.accSoft else if (dashed) Color.Transparent else c.card)
            .border(1.dp, if (selected) c.acc else if (dashed) c.muted else c.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) c.accText else if (dashed) c.muted else c.soft,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    val c = LocalVault.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.line, shape)
            .padding(4.dp)
    ) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (sel) c.acc else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    o, color = if (sel) c.accInk else c.muted, fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp, maxLines = 1
                )
            }
        }
    }
}

@Composable
fun IconSquare(icon: ImageVector, description: String, tint: Color? = null, onClick: () -> Unit) {
    val c = LocalVault.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(44.dp)
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.line, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint ?: c.soft, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val c = LocalVault.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconSquare(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            title, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        actions()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), color = LocalVault.current.muted, fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, modifier = modifier
    )
}

@Composable
fun ProgressLine(fraction: Float, modifier: Modifier = Modifier, color: Color = LocalVault.current.acc) {
    val c = LocalVault.current
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(c.bar2)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = LocalVault.current
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) c.acc else c.line)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) c.accInk else c.muted, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, color: Color? = null, onClick: () -> Unit) {
    val c = LocalVault.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(56.dp)
            .clip(shape)
            .background(c.card)
            .border(1.dp, c.line, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color ?: c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun Keypad(onKey: (String) -> Unit, onBackspace: () -> Unit) {
    val c = LocalVault.current
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫"))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { if (k == "⌫") onBackspace() else onKey(k) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            k, color = c.text, style = Num.copy(fontSize = 22.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

/** Applies a keypad key to an amount string, max 2 decimals. */
fun applyKey(current: String, key: String): String {
    if (key == ".") return if (current.contains(".")) current else "$current."
    val next = if (current == "0") key else current + key
    val parts = next.split(".")
    if (parts.size == 2 && parts[1].length > 2) return current
    if (parts[0].length > 12) return current
    return next
}

fun backspace(current: String): String = if (current.length <= 1) "0" else current.dropLast(1)

private val popular = listOf("NGN", "USD", "GBP", "EUR", "CAD", "GHS", "KES", "ZAR")

@Composable
fun CurrencyPickerDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val c = LocalVault.current
    var query by remember { mutableStateOf("") }
    val all = Money.allCurrencies
    val list = remember(query) {
        if (query.isBlank()) {
            all.filter { it.code in popular }.sortedBy { popular.indexOf(it.code) } +
                all.filter { it.code !in popular }
        } else {
            val q = query.trim().lowercase()
            all.filter { it.code.lowercase().contains(q) || it.name.lowercase().contains(q) }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(c.card)
                .padding(16.dp)
        ) {
            Text("Currency", color = c.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("Search e.g. naira, USD") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(list, key = { it.code }) { item ->
                    val sel = item.code == current
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) c.accSoft else Color.Transparent)
                            .clickable { onPick(item.code) }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Money.symbol(item.code).trim(), color = c.accText, fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(52.dp), maxLines = 1
                        )
                        Text(item.code, color = c.text, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(52.dp))
                        Text(item.name, color = c.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickDialog(initial: Long, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = Dates.toPickerMillis(initial))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Dates.fromPickerMillis(it)) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String = "",
    label: String = "",
    confirm: String = "Save",
    allowEmpty: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value, onValueChange = { value = it }, singleLine = true,
                label = { if (label.isNotEmpty()) Text(label) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = allowEmpty || value.isNotBlank(), onClick = { onConfirm(value.trim()) }) {
                Text(confirm)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String = "Delete",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = LocalVault.current.exp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    number: Boolean = false,
    password: Boolean = false,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = modifier,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                password -> KeyboardType.Password
                number -> KeyboardType.Decimal
                else -> KeyboardType.Text
            }
        )
    )
}

@Composable
fun SettingSwitch(
    title: String,
    subtitle: String = "",
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    val c = LocalVault.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) c.text else c.muted, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) Text(subtitle, color = c.muted, fontSize = 12.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.acc, checkedThumbColor = c.accInk,
                uncheckedTrackColor = c.bar2, uncheckedThumbColor = c.muted, uncheckedBorderColor = c.line
            )
        )
    }
}

@Composable
fun NavRow(title: String, subtitle: String = "", onClick: () -> Unit) {
    val c = LocalVault.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (subtitle.isNotEmpty()) Text(subtitle, color = c.muted, fontSize = 12.sp)
        }
        Text("›", color = c.muted, fontSize = 22.sp)
    }
}

@Composable
fun EmptyNote(text: String) {
    val c = LocalVault.current
    VCard(Modifier.fillMaxWidth()) {
        Text(text, color = c.muted, fontSize = 14.sp)
    }
}

@Composable
fun DialogTitleText(text: String) = Text(text, fontWeight = FontWeight.ExtraBold)
