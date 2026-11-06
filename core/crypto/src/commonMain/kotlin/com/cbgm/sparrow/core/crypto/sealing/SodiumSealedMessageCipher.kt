package com.cbgm.sparrow.core.crypto.sealing

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.ionspin.kotlin.crypto.aead.AuthenticatedEncryptionWithAssociatedData
import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.scalarmult.ScalarMultiplication
import com.ionspin.kotlin.crypto.util.LibsodiumRandom
import org.kotlincrypto.hash.sha2.SHA256

/**
 * `ephemeral X25519 public key || ChaCha20-Poly1305(message)` with the key
 * `SHA-256(context || shared secret || ephemeral public key || recipient public key)` and a zero nonce; every message
 * has its own ephemeral key. Replies are ChaCha20-Poly1305 with a zero nonce under a one-time reply key. The JDK on
 * the server has the same primitives.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class SodiumSealedMessageCipher : SealedMessageCipher {
    override suspend fun seal(
        message: ByteArray,
        recipientPublicKey: ByteArray,
        context: String
    ): Result<ByteArray> =
        runCatching {
            SodiumRuntime.initialize().getOrThrow()
            sealWith(message, recipientPublicKey, context, Box.keypair().secretKey.toByteArray())
        }

    override suspend fun openReply(
        sealed: ByteArray,
        replyKey: ByteArray
    ): ByteArray? =
        runCatching {
            SodiumRuntime.initialize().getOrThrow()
            AuthenticatedEncryptionWithAssociatedData
                .chaCha20Poly1305IetfDecrypt(sealed.toUByteArray(), UByteArray(0), UByteArray(NONCE_BYTES), replyKey.toUByteArray())
                .toByteArray()
        }.getOrNull()

    override suspend fun newReplyKey(): ByteArray {
        SodiumRuntime.initialize().getOrThrow()
        return LibsodiumRandom.buf(KEY_BYTES).toByteArray()
    }

    internal fun sealWith(
        message: ByteArray,
        recipientPublicKey: ByteArray,
        context: String,
        ephemeralSecretKey: ByteArray
    ): ByteArray {
        require(recipientPublicKey.size == KEY_BYTES) { "X25519 key must be $KEY_BYTES bytes" }
        val ephemeralPublicKey =
            ScalarMultiplication.scalarMultiplicationBase(ephemeralSecretKey.toUByteArray()).toByteArray()
        val shared =
            ScalarMultiplication
                .scalarMultiplication(ephemeralSecretKey.toUByteArray(), recipientPublicKey.toUByteArray())
                .toByteArray()
        require(shared.any { byte -> byte != 0.toByte() }) { "Not a valid X25519 key" }
        val key = SHA256().digest(context.encodeToByteArray() + shared + ephemeralPublicKey + recipientPublicKey)
        val ciphertext =
            AuthenticatedEncryptionWithAssociatedData.chaCha20Poly1305IetfEncrypt(
                message.toUByteArray(),
                UByteArray(0),
                UByteArray(NONCE_BYTES),
                key.toUByteArray()
            )
        return ephemeralPublicKey + ciphertext.toByteArray()
    }

    private companion object {
        const val KEY_BYTES = 32
        const val NONCE_BYTES = 12
    }
}
