package com.cbgm.securechat.server.security

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object Signatures {
    fun sign(
        content: ByteArray,
        privateKey: PrivateKey
    ): ByteArray =
        Signature.getInstance("Ed25519").run {
            initSign(privateKey)
            update(content)
            sign()
        }

    fun verify(
        content: ByteArray,
        signature: ByteArray,
        publicKey: PublicKey
    ): Boolean =
        Signature.getInstance("Ed25519").run {
            initVerify(publicKey)
            update(content)
            verify(signature)
        }

    fun decodePublicKey(encoded: ByteArray): PublicKey =
        KeyFactory
            .getInstance("Ed25519")
            .generatePublic(
                X509EncodedKeySpec(
                    if (encoded.size == RAW_ED25519_PUBLIC_KEY_BYTES) {
                        ED25519_X509_PREFIX + encoded
                    } else {
                        encoded
                    }
                )
            )

    private const val RAW_ED25519_PUBLIC_KEY_BYTES = 32

    private val ED25519_X509_PREFIX =
        byteArrayOf(
            0x30,
            0x2a,
            0x30,
            0x05,
            0x06,
            0x03,
            0x2b,
            0x65,
            0x70,
            0x03,
            0x21,
            0x00
        )
}
