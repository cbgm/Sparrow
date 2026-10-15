package com.cbgm.sparrow.feature.payment.device

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AES-256-GCM ciphertext only, in noBackupFilesDir; key never leaves Android Keystore. */
internal class AndroidBankDetailsStorage(
    context: Context
) : BankDetailsStorage {
    private val mutex = Mutex()
    private val directory = File(context.noBackupFilesDir, "payment-bank-details")
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    init {
        // Recover plaintext share files left behind by a previous process.
        File(context.cacheDir, "payment-share").listFiles()?.forEach { it.delete() }
    }

    override suspend fun read(key: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = file(key)
            if (!file.baseFile.exists()) return@withLock null
            val bytes = file.readFully()
            require(bytes.size in 30..4096 && bytes[0] == 1.toByte()) { "Invalid encrypted bank-details file" }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, requireNotNull(existingKey()), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            cipher.updateAAD(key.toByteArray(Charsets.UTF_8))
            val plaintext = cipher.doFinal(bytes.copyOfRange(13, bytes.size))
            try {
                plaintext.toString(Charsets.UTF_8)
            } finally {
                plaintext.fill(0)
            }
        }
    }

    override suspend fun write(key: String, value: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(directory.exists() || directory.mkdirs()) { "Secure payment directory unavailable" }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: generateKey())
            cipher.updateAAD(key.toByteArray(Charsets.UTF_8))
            val plaintext = value.toByteArray(Charsets.UTF_8)
            val ciphertext = try {
                cipher.doFinal(plaintext)
            } finally {
                plaintext.fill(0)
            }
            require(cipher.iv.size == 12)
            val file = file(key)
            val stream = file.startWrite()
            try {
                stream.write(byteArrayOf(1) + cipher.iv + ciphertext)
                file.finishWrite(stream)
            } catch (error: Throwable) {
                file.failWrite(stream)
                throw error
            }
        }
    }

    override suspend fun delete(key: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val file = file(key)
            file.delete()
            check(!file.baseFile.exists()) { "Saved bank details could not be deleted" }
        }
    }

    private fun file(key: String): AtomicFile {
        val name = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return AtomicFile(File(directory, "$name.bin"))
    }

    private fun existingKey(): SecretKey? = keyStore.getKey(KEY_ALIAS, null) as? SecretKey

    private fun generateKey(): SecretKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
        init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build()
        )
        generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "sparrow.payment.bank-details.v1"
    }
}
