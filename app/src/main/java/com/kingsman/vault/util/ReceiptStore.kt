package com.kingsman.vault.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.kingsman.vault.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Receipt photos are shrunk, re-encoded as JPEG (which also strips GPS/camera metadata),
 * encrypted with a Keystore key and stored in private app storage. Plain copies never stay on disk.
 */
class ReceiptStore(private val context: Context, private val security: SecurityManager) {

    private val dir: File get() = File(context.filesDir, "receipts").apply { mkdirs() }

    /** Temporary file the camera app writes into (deleted after import). */
    fun captureFile(): File {
        val d = File(context.cacheDir, "captures").apply { mkdirs() }
        return File(d, "capture.jpg").apply { if (exists()) delete() }
    }

    suspend fun importFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
        importBytes(bytes)
    }

    suspend fun importFromFile(file: File): String? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext null
        val bytes = file.readBytes()
        file.delete()
        importBytes(bytes)
    }

    private fun importBytes(raw: ByteArray): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > MAX_SIDE * 2 || bounds.outHeight / sample > MAX_SIDE * 2) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null

        val rotation = runCatching {
            when (ExifInterface(ByteArrayInputStream(raw)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)

        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height))
        val m = Matrix().apply {
            postScale(scale, scale)
            if (rotation != 0f) postRotate(rotation)
        }
        val finalBmp = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        val out = ByteArrayOutputStream()
        finalBmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
        if (finalBmp !== decoded) decoded.recycle()

        val name = UUID.randomUUID().toString() + ".enc"
        File(dir, name).writeBytes(security.encryptFile(out.toByteArray()))
        return name
    }

    suspend fun load(name: String): Bitmap? = withContext(Dispatchers.IO) {
        rawJpeg(name)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    /** Decrypted JPEG bytes (used for backups). */
    fun rawJpeg(name: String): ByteArray? = runCatching {
        val f = File(dir, name)
        if (!f.exists()) null else security.decryptFile(f.readBytes())
    }.getOrNull()

    /** Re-encrypt a JPEG from a backup with this phone's key. */
    fun restoreJpeg(name: String, jpeg: ByteArray) {
        File(dir, name).writeBytes(security.encryptFile(jpeg))
    }

    fun delete(name: String?) {
        if (name.isNullOrBlank()) return
        File(dir, name).delete()
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    companion object {
        private const val MAX_SIDE = 1600
    }
}
