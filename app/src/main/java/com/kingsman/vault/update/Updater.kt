package com.kingsman.vault.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.kingsman.vault.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class ReleaseInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256Url: String?,
    val publishedAt: String
)

/**
 * Auto-update through GitHub Releases.
 *
 * - Only talks to api.github.com / github.com, and sends nothing about you or your money.
 * - The downloaded APK is checked against the SHA-256 published with the release.
 * - Android itself refuses the update unless it is signed with the same key as the installed app,
 *   so a tampered APK can't replace Vault.
 * - Android always asks you to confirm the install (apps can't install silently).
 */
object Updater {

    private val API = "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest"

    /** Returns the latest release if it is newer than this app, otherwise null. */
    suspend fun checkForUpdate(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val conn = (URL(API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Vault-Android/${BuildConfig.VERSION_NAME}")
        }
        try {
            if (conn.responseCode == 404) return@withContext null // no releases yet
            if (conn.responseCode != 200) error("GitHub answered ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            if (json.optBoolean("draft") || json.optBoolean("prerelease")) return@withContext null
            val version = json.getString("tag_name").removePrefix("v").trim()
            if (compareVersions(version, BuildConfig.VERSION_NAME) <= 0) return@withContext null

            val assets = json.getJSONArray("assets")
            var apkUrl: String? = null
            var apkSize = 0L
            var shaUrl: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.getString("name")
                when {
                    name.endsWith(".apk") -> {
                        apkUrl = a.getString("browser_download_url")
                        apkSize = a.optLong("size")
                    }
                    name.endsWith(".apk.sha256") -> shaUrl = a.getString("browser_download_url")
                }
            }
            val url = apkUrl ?: return@withContext null
            ReleaseInfo(
                version = version,
                notes = json.optString("body").trim(),
                apkUrl = url,
                apkSize = apkSize,
                sha256Url = shaUrl,
                publishedAt = json.optString("published_at")
            )
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads and verifies the APK. [onProgress] gets 0f..1f. */
    suspend fun download(context: Context, release: ReleaseInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "Vault-${release.version}.apk")

            val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Vault-Android/${BuildConfig.VERSION_NAME}")
            }
            try {
                if (conn.responseCode != 200) error("Download failed (${conn.responseCode})")
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
                val digest = MessageDigest.getInstance("SHA-256")
                conn.inputStream.use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var read: Int
                        var done = 0L
                        while (input.read(buf).also { read = it } >= 0) {
                            out.write(buf, 0, read)
                            digest.update(buf, 0, read)
                            done += read
                            if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                release.sha256Url?.let { shaUrl ->
                    val expected = URL(shaUrl).readText().trim().split(Regex("\\s+")).first().lowercase()
                    if (expected != actual) {
                        file.delete()
                        error("Download didn't match the published checksum — not installing.")
                    }
                }
                file
            } finally {
                conn.disconnect()
            }
        }

    /** True when Vault is allowed to open the system installer. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Opens the "Install unknown apps" setting for Vault. */
    fun permissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** Hands the verified APK to Android's installer, which asks you to confirm. */
    fun installIntent(context: Context, apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** "1.10.0" > "1.9.3". Non-numeric parts count as 0. */
    fun compareVersions(a: String, b: String): Int {
        val pa = a.split(".", "-").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".", "-").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
