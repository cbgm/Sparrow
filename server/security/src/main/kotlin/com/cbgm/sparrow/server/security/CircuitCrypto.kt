package com.cbgm.sparrow.server.security

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.ChaCha20ParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** The two keys of one circuit hop: [forward] for cells towards the session node, [backward] for cells back. */
class CircuitHopKeys(
    val forward: ByteArray,
    val backward: ByteArray
) {
    override fun toString(): String = "CircuitHopKeys(redacted)"
}

/** What a relay answers a circuit hop handshake with: `relay ephemeral key || confirmation`, and the hop's keys. */
class CircuitHandshakeReply(
    val reply: ByteArray,
    val keys: CircuitHopKeys
)

/**
 * The handshake that adds one hop to a circuit, one round trip. The app sends an ephemeral X25519 key X; the relay
 * answers with its own ephemeral key Y and a confirmation. Both derive
 * `k = SHA-256(context || DH(y, X) || DH(b, X) || X || Y || B)` with the relay's published relay key B:
 * the ephemeral pair gives forward secrecy, the relay key proves the app talks to the relay it chose (only that relay
 * can compute the confirmation `SHA-256(k || "auth")`). The app does the same with libsodium.
 */
object CircuitHandshake {
    const val KEY_BYTES = 32
    const val REPLY_BYTES = 2 * KEY_BYTES
    private const val CONTEXT = "sparrow-circuit-v1"

    /** Null when [clientEphemeralKey] is not a usable X25519 key. */
    fun respond(
        relayKeys: X25519KeyPair,
        clientEphemeralKey: ByteArray
    ): CircuitHandshakeReply? {
        if (clientEphemeralKey.size != KEY_BYTES) return null
        val ephemeral = X25519KeyPair.generate()
        return try {
            val ephemeralShared = agree(ephemeral, clientEphemeralKey)
            val staticShared = agree(relayKeys, clientEphemeralKey)
            val secret =
                sha256(
                    CONTEXT.encodeToByteArray(),
                    ephemeralShared,
                    staticShared,
                    clientEphemeralKey,
                    ephemeral.publicKey,
                    relayKeys.publicKey
                )
            CircuitHandshakeReply(
                reply = ephemeral.publicKey + sha256(secret, "auth".encodeToByteArray()),
                keys =
                    CircuitHopKeys(
                        forward = sha256(secret, "forward".encodeToByteArray()),
                        backward = sha256(secret, "backward".encodeToByteArray())
                    )
            )
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    /** The app's side, here for nodes and tests: a fresh ephemeral key to send. */
    fun initiate(): X25519KeyPair = X25519KeyPair.generate()

    /** The app's side: the hop's keys from the relay's [reply], or null when the relay did not prove its relay key. */
    fun complete(
        ephemeral: X25519KeyPair,
        relayPublicKey: ByteArray,
        reply: ByteArray
    ): CircuitHopKeys? {
        if (reply.size != REPLY_BYTES || relayPublicKey.size != KEY_BYTES) return null
        val relayEphemeralKey = reply.copyOfRange(0, KEY_BYTES)
        return try {
            val secret =
                sha256(
                    CONTEXT.encodeToByteArray(),
                    agree(ephemeral, relayEphemeralKey),
                    agree(ephemeral, relayPublicKey),
                    ephemeral.publicKey,
                    relayEphemeralKey,
                    relayPublicKey
                )
            val confirmed =
                MessageDigest.isEqual(sha256(secret, "auth".encodeToByteArray()), reply.copyOfRange(KEY_BYTES, REPLY_BYTES))
            CircuitHopKeys(
                forward = sha256(secret, "forward".encodeToByteArray()),
                backward = sha256(secret, "backward".encodeToByteArray())
            ).takeIf { confirmed }
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    private fun agree(
        own: X25519KeyPair,
        otherPublicKey: ByteArray
    ): ByteArray {
        val shared =
            KeyAgreement.getInstance(X25519).run {
                init(own.privateKey)
                doPhase(KeyFactory.getInstance(X25519).generatePublic(X509EncodedKeySpec(X509_PREFIX + otherPublicKey)), true)
                generateSecret()
            }
        if (shared.all { byte -> byte == 0.toByte() }) throw GeneralSecurityException("Not a valid X25519 key")
        return shared
    }
}

/**
 * The per-hop encryption of circuit cells. A cell is for the circuit's last hop only: that hop seals and opens it
 * with ChaCha20-Poly1305 ([seal], [open]); every hop before it adds or removes a ChaCha20 layer ([stream]) that keeps
 * the cell's size. Each direction of each hop counts its cells; the count is the nonce, so no nonce repeats.
 */
object CircuitLayer {
    const val TAG_BYTES = 16

    fun stream(
        key: ByteArray,
        counter: Long,
        data: ByteArray
    ): ByteArray =
        Cipher.getInstance("ChaCha20").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "ChaCha20"), ChaCha20ParameterSpec(nonce(counter), 0))
            doFinal(data)
        }

    fun seal(
        key: ByteArray,
        counter: Long,
        data: ByteArray
    ): ByteArray = aead(Cipher.ENCRYPT_MODE, key, counter, data)

    /** Null when the cell was not sealed with [key] and [counter], or changed on the way. */
    fun open(
        key: ByteArray,
        counter: Long,
        data: ByteArray
    ): ByteArray? =
        try {
            aead(Cipher.DECRYPT_MODE, key, counter, data)
        } catch (_: GeneralSecurityException) {
            null
        }

    private fun aead(
        mode: Int,
        key: ByteArray,
        counter: Long,
        data: ByteArray
    ): ByteArray =
        Cipher.getInstance("ChaCha20-Poly1305").run {
            init(mode, SecretKeySpec(key, "ChaCha20"), IvParameterSpec(nonce(counter)))
            doFinal(data)
        }

    /** 4 zero bytes, then the counter as 8 bytes, big-endian. */
    private fun nonce(counter: Long): ByteArray = ByteBuffer.allocate(NONCE_BYTES).putInt(0).putLong(counter).array()
}

private fun sha256(vararg parts: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").run {
        parts.forEach(::update)
        digest()
    }

private const val X25519 = "X25519"
private const val NONCE_BYTES = 12
private val X509_PREFIX = byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00)
