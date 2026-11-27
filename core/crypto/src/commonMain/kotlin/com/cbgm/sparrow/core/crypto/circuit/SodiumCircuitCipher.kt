package com.cbgm.sparrow.core.crypto.circuit

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.ionspin.kotlin.crypto.aead.AuthenticatedEncryptionWithAssociatedData
import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.scalarmult.ScalarMultiplication
import com.ionspin.kotlin.crypto.stream.Stream
import org.kotlincrypto.hash.sha2.SHA256

/**
 * [CircuitCipher] with libsodium: `k = SHA-256(context || DH(x, Y) || DH(x, B) || X || Y || B)`, the confirmation
 * `SHA-256(k || "auth")`, the hop keys `SHA-256(k || "forward")` and `SHA-256(k || "backward")`. Nonces are 4 zero
 * bytes and the cell counter as 8 bytes, big-endian.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class SodiumCircuitCipher : CircuitCipher {
    override suspend fun newEphemeralKey(): CircuitEphemeralKey {
        SodiumRuntime.initialize().getOrThrow()
        val keyPair = Box.keypair()
        return CircuitEphemeralKey(keyPair.secretKey.toByteArray(), keyPair.publicKey.toByteArray())
    }

    override suspend fun completeHandshake(
        ephemeralKey: CircuitEphemeralKey,
        relayPublicKey: ByteArray,
        reply: ByteArray
    ): CircuitHopKeys? {
        SodiumRuntime.initialize().getOrThrow()
        if (reply.size != REPLY_BYTES || relayPublicKey.size != KEY_BYTES) return null
        val relayEphemeralKey = reply.copyOfRange(0, KEY_BYTES)
        val ephemeralShared = agree(ephemeralKey.secretKey, relayEphemeralKey) ?: return null
        val staticShared = agree(ephemeralKey.secretKey, relayPublicKey) ?: return null
        val secret =
            sha256(
                CONTEXT.encodeToByteArray(),
                ephemeralShared,
                staticShared,
                ephemeralKey.publicKey,
                relayEphemeralKey,
                relayPublicKey
            )
        val confirmation = sha256(secret, "auth".encodeToByteArray())
        if (!constantTimeEquals(confirmation, reply.copyOfRange(KEY_BYTES, REPLY_BYTES))) return null
        return CircuitHopKeys(
            forward = sha256(secret, "forward".encodeToByteArray()),
            backward = sha256(secret, "backward".encodeToByteArray())
        )
    }

    override suspend fun stream(
        key: ByteArray,
        counter: Long,
        data: ByteArray
    ): ByteArray {
        SodiumRuntime.initialize().getOrThrow()
        return Stream.chacha20IetfXor(data.toUByteArray(), nonce(counter), key.toUByteArray()).toByteArray()
    }

    override suspend fun seal(
        key: ByteArray,
        counter: Long,
        data: ByteArray
    ): ByteArray {
        SodiumRuntime.initialize().getOrThrow()
        return AuthenticatedEncryptionWithAssociatedData
            .chaCha20Poly1305IetfEncrypt(data.toUByteArray(), UByteArray(0), nonce(counter), key.toUByteArray())
            .toByteArray()
    }

    override suspend fun open(
        key: ByteArray,
        counter: Long,
        sealed: ByteArray
    ): ByteArray? =
        runCatching {
            SodiumRuntime.initialize().getOrThrow()
            AuthenticatedEncryptionWithAssociatedData
                .chaCha20Poly1305IetfDecrypt(sealed.toUByteArray(), UByteArray(0), nonce(counter), key.toUByteArray())
                .toByteArray()
        }.getOrNull()

    private fun agree(
        secretKey: ByteArray,
        publicKey: ByteArray
    ): ByteArray? =
        ScalarMultiplication
            .scalarMultiplication(secretKey.toUByteArray(), publicKey.toUByteArray())
            .toByteArray()
            .takeIf { shared -> shared.any { byte -> byte != 0.toByte() } }

    private fun sha256(vararg parts: ByteArray): ByteArray =
        SHA256().apply { parts.forEach { part -> update(part) } }.digest()

    private fun constantTimeEquals(
        first: ByteArray,
        second: ByteArray
    ): Boolean =
        first.size == second.size &&
            first.indices.fold(0) { difference, index -> difference or (first[index].toInt() xor second[index].toInt()) } == 0

    private fun nonce(counter: Long): UByteArray =
        UByteArray(NONCE_BYTES).also { nonce ->
            (0 until COUNTER_BYTES).forEach { index ->
                nonce[NONCE_BYTES - 1 - index] = (counter ushr (8 * index)).toUByte()
            }
        }

    private companion object {
        const val CONTEXT = "sparrow-circuit-v1"
        const val KEY_BYTES = 32
        const val REPLY_BYTES = 2 * KEY_BYTES
        const val NONCE_BYTES = 12
        const val COUNTER_BYTES = 8
    }
}
