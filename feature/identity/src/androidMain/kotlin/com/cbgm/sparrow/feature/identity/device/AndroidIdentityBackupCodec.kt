package com.cbgm.sparrow.feature.identity.device

import android.util.Base64
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackup
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Self-contained, versioned portable backup; Android Keystore is NOT involved in file decryption. */
class AndroidIdentityBackupCodec : IdentityBackupCodec {
    override fun encrypt(backup: IdentityBackup, password: CharArray): ByteArray {
        require(password.size >= 12) { "Use at least 12 characters for your backup password" }
        require(
            backup.publicIdentity.encryptionPublicKey.size == 32 && backup.encryptionPrivateKey.size == 32 &&
                backup.publicIdentity.signingPublicKey.size == 32 && backup.signingPrivateKey.size == 64
        )
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val saltString = salt.base64()
        val nonceString = nonce.base64()
        val data = ByteBuffer.allocate(160)
            .put(backup.publicIdentity.encryptionPublicKey)
            .put(backup.publicIdentity.signingPublicKey)
            .put(backup.encryptionPrivateKey)
            .put(backup.signingPrivateKey)
            .array()
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val key = deriveKey(password, salt, ITERATIONS)
            try {
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                cipher.updateAAD(aad(saltString, nonceString, ITERATIONS))
                val ciphertext = cipher.doFinal(data)
                return JSONObject().apply {
                    put("format", FORMAT)
                    put("version", VERSION)
                    put("kdf", "PBKDF2-HMAC-SHA256")
                    put("iterations", ITERATIONS)
                    put("salt", saltString)
                    put("cipher", "AES-256-GCM")
                    put("nonce", nonceString)
                    put("ciphertext", ciphertext.base64())
                }.toString(2).toByteArray(Charsets.UTF_8)
            } finally {
                key.fill(0)
            }
        } finally {
            data.fill(0)
            salt.fill(0)
            nonce.fill(0)
        }
    }

    override fun decrypt(document: ByteArray, password: CharArray): IdentityBackup {
        require(document.size in 100..MAX_JSON_BYTES) { "Identity backup file is invalid or too large" }
        require(password.isNotEmpty()) { "Enter the backup password" }
        val json = try {
            JSONObject(String(document, Charsets.UTF_8))
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid identity backup JSON", e)
        }
        require(
            json.optString("format") == FORMAT && json.optInt("version", -1) == VERSION &&
                json.optString("kdf") == "PBKDF2-HMAC-SHA256" &&
                json.optString("cipher") == "AES-256-GCM"
        ) { "Unsupported identity backup format or version" }
        val iterations = json.optInt("iterations", -1)
        require(iterations in ITERATIONS..MAX_ITERATIONS) { "Invalid backup password-derivation parameters" }
        val saltString = json.getString("salt")
        val nonceString = json.getString("nonce")
        val salt = parseBase64(saltString, 16)
        val nonce = parseBase64(nonceString, 12)
        val ciphertext = parseBase64(json.getString("ciphertext"), 176)
        try {
            val key = deriveKey(password, salt, iterations)
            val plain = try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
                cipher.updateAAD(aad(saltString, nonceString, iterations))
                cipher.doFinal(ciphertext)
            } catch (e: javax.crypto.AEADBadTagException) {
                throw IllegalArgumentException("Wrong backup password or damaged file", e)
            } finally {
                key.fill(0)
            }
            try {
                require(plain.size == 160) { "Invalid identity backup payload" }
                val bytes = ByteBuffer.wrap(plain)

                fun next(n: Int) = ByteArray(n).also { bytes.get(it) }
                val encryptionPublic = next(32)
                val signingPublic = next(32)
                val encryptionPrivate = next(32)
                val signingPrivate = next(64)
                return IdentityBackup(
                    PublicIdentity(encryptionPublic, signingPublic),
                    encryptionPrivate,
                    signingPrivate
                )
            } finally {
                plain.fill(0)
            }
        } finally {
            salt.fill(0)
            nonce.fill(0)
            ciphertext.fill(0)
        }
    }

    private fun aad(salt: String, nonce: String, iterations: Int): ByteArray =
        "$FORMAT|$VERSION|PBKDF2-HMAC-SHA256|$iterations|$salt|AES-256-GCM|$nonce".toByteArray(Charsets.UTF_8)

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun parseBase64(value: String, exactSize: Int): ByteArray {
        require(value.length <= exactSize * 2) { "Invalid backup field size" }
        val bytes = try {
            Base64.decode(value, Base64.NO_WRAP)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid backup encoding", e)
        }
        require(bytes.size == exactSize) { "Invalid backup field length" }
        return bytes
    }

    private fun ByteArray.base64() = Base64.encodeToString(this, Base64.NO_WRAP)

    private companion object {
        const val FORMAT = "sparrow-identity-backup"
        const val VERSION = 1
        const val ITERATIONS = 310_000
        const val MAX_ITERATIONS = 1_000_000
        const val MAX_JSON_BYTES = 16_384
        val random = SecureRandom()
    }
}
