package com.kingsman.vault.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.kingsman.vault.data.Prefs
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * - PIN: salted PBKDF2-HMAC-SHA256 hash (the PIN itself is never stored).
 * - Database: SQLCipher with a random 256-bit key, wrapped by an AES key held in the Android Keystore.
 * - Backups: AES-256-GCM with a key derived from the backup password.
 */
class SecurityManager(private val prefs: Prefs) {

    private val random = SecureRandom()

    // ---------------- PIN ----------------
    fun isPinSet(): Boolean = prefs.pinHash != null

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        prefs.pinSalt = b64(salt)
        prefs.pinHash = b64(pbkdf2(pin.toCharArray(), salt, PIN_ITERATIONS))
        prefs.pinLength = pin.length
        prefs.failedAttempts = 0
        prefs.lockoutUntil = 0L
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.pinSalt?.let { unb64(it) } ?: return false
        val stored = prefs.pinHash?.let { unb64(it) } ?: return false
        val attempt = pbkdf2(pin.toCharArray(), salt, PIN_ITERATIONS)
        return MessageDigest.isEqual(stored, attempt)
    }

    // ---------------- Database key ----------------
    fun databasePassphrase(): ByteArray {
        val wrapped = prefs.dbKeyWrapped
        val iv = prefs.dbKeyIv
        if (wrapped != null && iv != null) {
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, unb64(iv)))
            return cipher.doFinal(unb64(wrapped))
        }
        val raw = ByteArray(32).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        prefs.dbKeyIv = b64(cipher.iv)
        prefs.dbKeyWrapped = b64(cipher.doFinal(raw))
        return raw
    }

    private fun keystoreKey(alias: String = KEY_ALIAS): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun deleteKeys() {
        runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            ks.deleteEntry(KEY_ALIAS)
            ks.deleteEntry(FILE_KEY_ALIAS)
        }
    }

    // ---------------- Receipt photo encryption ----------------
    /** Output = 12-byte IV + AES-GCM ciphertext, with a key that never leaves the Keystore. */
    fun encryptFile(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey(FILE_KEY_ALIAS))
        return cipher.iv + cipher.doFinal(plain)
    }

    fun decryptFile(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(
            Cipher.DECRYPT_MODE, keystoreKey(FILE_KEY_ALIAS),
            GCMParameterSpec(128, data.copyOfRange(0, 12))
        )
        return cipher.doFinal(data.copyOfRange(12, data.size))
    }

    // ---------------- Backup encryption ----------------
    fun encryptBackup(plain: ByteArray, password: String): ByteArray {
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(12).also { random.nextBytes(it) }
        val key = SecretKeySpec(pbkdf2(password.toCharArray(), salt, BACKUP_ITERATIONS), "AES")
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        return MAGIC + salt + iv + cipher.doFinal(plain)
    }

    /** Throws if the password is wrong or the file is not a Vault backup. */
    fun decryptBackup(data: ByteArray, password: String): ByteArray {
        require(data.size > MAGIC.size + 28) { "Not a Vault backup file" }
        require(data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) { "Not a Vault backup file" }
        var p = MAGIC.size
        val salt = data.copyOfRange(p, p + 16); p += 16
        val iv = data.copyOfRange(p, p + 12); p += 12
        val key = SecretKeySpec(pbkdf2(password.toCharArray(), salt, BACKUP_ITERATIONS), "AES")
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(data.copyOfRange(p, data.size))
    }

    // ---------------- helpers ----------------
    private fun pbkdf2(pw: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pw, salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)

    companion object {
        private const val KEY_ALIAS = "vault_db_wrap_key"
        private const val FILE_KEY_ALIAS = "vault_file_key"
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val PIN_ITERATIONS = 120_000
        private const val BACKUP_ITERATIONS = 200_000
        private val MAGIC = "VAULT1".toByteArray()
    }
}
