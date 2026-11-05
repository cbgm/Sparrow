package com.cbgm.sparrow.core.crypto.claim

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.ionspin.kotlin.crypto.aead.AuthenticatedEncryptionWithAssociatedData
import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.scalarmult.ScalarMultiplication
import org.kotlincrypto.hash.sha2.SHA256

/**
 * `ephemeral X25519 public key || ChaCha20-Poly1305(claim)` with the key
 * `SHA-256("sparrow-alias-claim-v1" || shared secret || ephemeral public key || presence public key)` and a zero
 * nonce; every claim has its own ephemeral key. The JDK on the server has the same primitives.
 */
class SodiumAliasClaimSealer : AliasClaimSealer {
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun seal(
        claim: ByteArray,
        presencePublicKey: ByteArray
    ): Result<ByteArray> =
        runCatching {
            SodiumRuntime.initialize().getOrThrow()
            sealWith(claim, presencePublicKey, Box.keypair().secretKey.toByteArray())
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    internal fun sealWith(
        claim: ByteArray,
        presencePublicKey: ByteArray,
        ephemeralSecretKey: ByteArray
    ): ByteArray {
        require(presencePublicKey.size == KEY_BYTES) { "Alias claim key must be $KEY_BYTES bytes" }
        val ephemeralPublicKey =
            ScalarMultiplication.scalarMultiplicationBase(ephemeralSecretKey.toUByteArray()).toByteArray()
        val shared =
            ScalarMultiplication
                .scalarMultiplication(ephemeralSecretKey.toUByteArray(), presencePublicKey.toUByteArray())
                .toByteArray()
        require(shared.any { byte -> byte != 0.toByte() }) { "Alias claim key is not a valid X25519 key" }
        val key = SHA256().digest(CONTEXT + shared + ephemeralPublicKey + presencePublicKey)
        val ciphertext =
            AuthenticatedEncryptionWithAssociatedData.chaCha20Poly1305IetfEncrypt(
                claim.toUByteArray(),
                UByteArray(0),
                UByteArray(NONCE_BYTES),
                key.toUByteArray()
            )
        return ephemeralPublicKey + ciphertext.toByteArray()
    }

    private companion object {
        const val KEY_BYTES = 32
        const val NONCE_BYTES = 12
        val CONTEXT = "sparrow-alias-claim-v1".encodeToByteArray()
    }
}
