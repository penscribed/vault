package com.kingsman.vault.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kingsman.vault.ui.IconSquare
import com.kingsman.vault.ui.VaultViewModel
import com.kingsman.vault.ui.theme.LocalVault

/** Full-screen view of an encrypted receipt photo (decrypted in memory only). */
@Composable
fun ReceiptViewerDialog(vm: VaultViewModel, name: String, onDismiss: () -> Unit) {
    val c = LocalVault.current
    var bmp by remember { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(name) {
        val b = vm.loadReceipt(name)
        if (b == null) failed = true else bmp = b.asImageBitmap()
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xF2000000)),
            contentAlignment = Alignment.Center
        ) {
            val image = bmp
            when {
                image != null -> Image(
                    bitmap = image, contentDescription = "Receipt photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
                failed -> Text("This receipt photo couldn't be opened.", color = Color.White)
                else -> CircularProgressIndicator(color = c.acc)
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                IconSquare(Icons.Rounded.Close, "Close", onClick = onDismiss)
            }
        }
    }
}
