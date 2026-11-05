package com.cbgm.sparrow.server.presence

import com.cbgm.sparrow.server.protocol.AliasClaim
import com.cbgm.sparrow.server.protocol.ClientRouteRegistration
import com.cbgm.sparrow.server.protocol.UnsignedAliasClaim
import com.cbgm.sparrow.server.protocol.serverJson
import com.cbgm.sparrow.server.protocol.unsigned
import com.cbgm.sparrow.server.security.Signatures
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The presence directory's key for alias claims. Apps seal their phone aliases for [publicKey] so the gateway that
 * forwards them cannot read them: `ephemeral X25519 public key || ChaCha20-Poly1305(claim)`, with the key
 * `SHA-256("sparrow-alias-claim-v1" || shared secret || ephemeral public key || publicKey)` and a zero nonce (every
 * claim has its own ephemeral key). The key lives only in memory: a restart replaces it, and apps pick up the new one
 * with the next signed node directory.
 */
internal class AliasClaimKeys private constructor(
    private val privateKey: PrivateKey,
    val publicKey: ByteArray
) {
    /** Returns the claim, or null when it was sealed for another key or changed on the way. */
    fun open(sealed: ByteArray): ByteArray? {
        if (sealed.size <= RAW_KEY_BYTES + TAG_BYTES) return null
        val ephemeralPublicKey = sealed.copyOfRange(0, RAW_KEY_BYTES)
        return try {
            val shared =
                KeyAgreement.getInstance(X25519).run {
                    init(privateKey)
                    doPhase(decodePublicKey(ephemeralPublicKey), true)
                    generateSecret()
                }
            Cipher.getInstance(CIPHER).run {
                init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(sealingKey(shared, ephemeralPublicKey, publicKey), "ChaCha20"),
                    IvParameterSpec(ByteArray(NONCE_BYTES))
                )
                doFinal(sealed, RAW_KEY_BYTES, sealed.size - RAW_KEY_BYTES)
            }
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    companion object {
        fun generate(): AliasClaimKeys {
            val keyPair = KeyPairGenerator.getInstance(X25519).generateKeyPair()
            return AliasClaimKeys(keyPair.private, keyPair.public.encoded.takeLast(RAW_KEY_BYTES).toByteArray())
        }

        /** For tests: a key from its raw 32 private and public bytes. */
        internal fun fromRaw(
            privateKey: ByteArray,
            publicKey: ByteArray
        ): AliasClaimKeys =
            AliasClaimKeys(
                KeyFactory.getInstance(X25519).generatePrivate(PKCS8EncodedKeySpec(PKCS8_PREFIX + privateKey)),
                publicKey
            )

        internal fun sealingKey(
            shared: ByteArray,
            ephemeralPublicKey: ByteArray,
            recipientPublicKey: ByteArray
        ): ByteArray =
            MessageDigest.getInstance("SHA-256").run {
                update(CONTEXT)
                update(shared)
                update(ephemeralPublicKey)
                update(recipientPublicKey)
                digest()
            }

        internal fun decodePublicKey(raw: ByteArray) =
            KeyFactory.getInstance(X25519).generatePublic(X509EncodedKeySpec(X509_PREFIX + raw))

        private const val X25519 = "X25519"
        private const val CIPHER = "ChaCha20-Poly1305"
        private const val RAW_KEY_BYTES = 32
        private const val TAG_BYTES = 16
        internal const val NONCE_BYTES = 12
        private val CONTEXT = "sparrow-alias-claim-v1".encodeToByteArray()
        private val X509_PREFIX =
            byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00)
        private val PKCS8_PREFIX =
            byteArrayOf(0x30, 0x2e, 0x02, 0x01, 0x00, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x04, 0x22, 0x04, 0x20)
    }
}

/** The registration's alias claim when it was sealed for this key and signed for this very route; otherwise null. */
internal fun AliasClaimKeys.claimFor(registration: ClientRouteRegistration): UnsignedAliasClaim? {
    val route = registration.route
    val claim =
        registration.sealedAliasClaim
            ?.let(::open)
            ?.let { plaintext -> runCatching { serverJson.decodeFromString<AliasClaim>(plaintext.decodeToString()) } }
            ?.getOrNull()
            ?: return null
    val boundToRoute =
        claim.routingId == route.routingId &&
            claim.connectionId == route.connectionId &&
            claim.generation == route.generation
    val signed =
        runCatching {
            Signatures.verify(
                content = serverJson.encodeToString(claim.unsigned()).encodeToByteArray(),
                signature = claim.signature,
                publicKey = Signatures.decodePublicKey(registration.clientSigningPublicKey)
            )
        }.getOrDefault(false)
    return claim.unsigned().takeIf { boundToRoute && signed }
}
