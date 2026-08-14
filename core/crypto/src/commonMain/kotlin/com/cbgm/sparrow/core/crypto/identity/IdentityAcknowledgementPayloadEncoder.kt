package com.cbgm.sparrow.core.crypto.identity

import com.cbgm.sparrow.core.crypto.util.ByteArrays

class IdentityAcknowledgementPayloadEncoder {
    fun encode(
        acknowledgedEncryptionPublicKey: ByteArray,
        acknowledgedSigningPublicKey: ByteArray,
        senderSigningPublicKey: ByteArray
    ): ByteArray {
        require(acknowledgedEncryptionPublicKey.isNotEmpty()) {
            "Acknowledged encryption key must not be empty"
        }

        require(acknowledgedSigningPublicKey.isNotEmpty()) {
            "Acknowledged signing key must not be empty"
        }

        require(senderSigningPublicKey.isNotEmpty()) {
            "Sender signing key must not be empty"
        }

        return ByteArrays.concatenate(
            DOMAIN_SEPARATOR.encodeToByteArray(),
            ByteArrays.encodeInt(PROTOCOL_VERSION),
            ByteArrays.withLengthPrefix(acknowledgedEncryptionPublicKey),
            ByteArrays.withLengthPrefix(acknowledgedSigningPublicKey),
            ByteArrays.withLengthPrefix(senderSigningPublicKey)
        )
    }

    private companion object {
        const val DOMAIN_SEPARATOR = "Sparrow.IdentityAcknowledgement"
        const val PROTOCOL_VERSION = 1
    }
}
