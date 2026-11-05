package com.cbgm.sparrow.server.security

import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.NamedParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * An X25519 key that sealed messages are opened with. A sealed message is
 * `ephemeral X25519 public key || ChaCha20-Poly1305(message)` with the key
 * `SHA-256(context || shared secret || ephemeral public key || recipient public key)` and a zero nonce: every
 * message has its own ephemeral key. The app seals with libsodium (`SodiumSealing`), which has the same primitives.
 */
class X25519KeyPair private constructor(
    internal val privateKey: PrivateKey,
    /** Raw 32 bytes. */
    val publicKey: ByteArray
) {
    /** Returns the message, or null when it was sealed for another key or context, or changed on the way. */
    fun open(
        sealed: ByteArray,
        context: String
    ): ByteArray? {
        if (sealed.size <= RAW_KEY_BYTES + TAG_BYTES) return null
        val ephemeralPublicKey = sealed.copyOfRange(0, RAW_KEY_BYTES)
        return try {
            val shared =
                KeyAgreement.getInstance(X25519).run {
                    init(privateKey)
                    doPhase(decodeX25519PublicKey(ephemeralPublicKey), true)
                    generateSecret()
                }
            chaCha20Poly1305(
                mode = Cipher.DECRYPT_MODE,
                key = sealingKey(context, shared, ephemeralPublicKey, publicKey),
                input = sealed.copyOfRange(RAW_KEY_BYTES, sealed.size)
            )
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    override fun toString(): String = "X25519KeyPair(redacted)"

    companion object {
        fun generate(): X25519KeyPair = fromSeed(ByteArray(RAW_KEY_BYTES).also(SecureRandom()::nextBytes))

        /** The key whose raw private bytes are [seed]; the same seed always gives the same key. */
        fun fromSeed(seed: ByteArray): X25519KeyPair {
            require(seed.size == RAW_KEY_BYTES) { "X25519 seed must be $RAW_KEY_BYTES bytes" }
            val keyPair =
                KeyPairGenerator.getInstance(X25519).run {
                    initialize(NamedParameterSpec.X25519, SeedRandom(seed))
                    generateKeyPair()
                }
            return X25519KeyPair(keyPair.private, keyPair.public.encoded.takeLast(RAW_KEY_BYTES).toByteArray())
        }
    }
}

object X25519Sealing {
    /** Seals [message] for [recipientPublicKey] (raw 32 bytes); see [X25519KeyPair]. */
    fun seal(
        message: ByteArray,
        recipientPublicKey: ByteArray,
        context: String
    ): ByteArray {
        val ephemeral = X25519KeyPair.generate()
        val shared =
            KeyAgreement.getInstance(X25519).run {
                init(ephemeral.privateKey)
                doPhase(decodeX25519PublicKey(recipientPublicKey), true)
                generateSecret()
            }
        val key = sealingKey(context, shared, ephemeral.publicKey, recipientPublicKey)
        return ephemeral.publicKey + chaCha20Poly1305(Cipher.ENCRYPT_MODE, key, message)
    }
}

/**
 * Replies to a sealed request: ChaCha20-Poly1305 with a zero nonce under a fresh 32-byte key the requester put into
 * the request, so only the requester can read the reply. A key is used for one reply only.
 */
object ReplySealing {
    const val KEY_BYTES = 32

    fun seal(
        message: ByteArray,
        key: ByteArray
    ): ByteArray {
        require(key.size == KEY_BYTES) { "Reply key must be $KEY_BYTES bytes" }
        return chaCha20Poly1305(Cipher.ENCRYPT_MODE, key, message)
    }

    fun open(
        sealed: ByteArray,
        key: ByteArray
    ): ByteArray? =
        try {
            chaCha20Poly1305(Cipher.DECRYPT_MODE, key, sealed)
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
}

/** The key this node relays onion layers with; gateway and federation derive the same one from the node identity. */
fun NodeIdentity.relayKeyPair(): X25519KeyPair =
    X25519KeyPair.fromSeed(
        MessageDigest.getInstance("SHA-256").run {
            update("sparrow-relay-key-v1".encodeToByteArray())
            update(privateKey.encoded)
            digest()
        }
    )

private fun sealingKey(
    context: String,
    shared: ByteArray,
    ephemeralPublicKey: ByteArray,
    recipientPublicKey: ByteArray
): ByteArray =
    MessageDigest.getInstance("SHA-256").run {
        update(context.encodeToByteArray())
        update(shared)
        update(ephemeralPublicKey)
        update(recipientPublicKey)
        digest()
    }

private fun chaCha20Poly1305(
    mode: Int,
    key: ByteArray,
    input: ByteArray
): ByteArray =
    Cipher.getInstance("ChaCha20-Poly1305").run {
        init(mode, SecretKeySpec(key, "ChaCha20"), IvParameterSpec(ByteArray(NONCE_BYTES)))
        doFinal(input)
    }

private fun decodeX25519PublicKey(raw: ByteArray) =
    KeyFactory.getInstance(X25519).generatePublic(X509EncodedKeySpec(X509_PREFIX + raw))

/** Hands the seed to the key generator as its only random bytes. */
private class SeedRandom(
    private val seed: ByteArray
) : SecureRandom() {
    override fun nextBytes(bytes: ByteArray) {
        seed.copyInto(bytes, endIndex = minOf(seed.size, bytes.size))
    }
}

private const val X25519 = "X25519"
private const val RAW_KEY_BYTES = 32
private const val TAG_BYTES = 16
private const val NONCE_BYTES = 12
private val X509_PREFIX = byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00)
