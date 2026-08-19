package com.cbgm.sparrow.core.crypto.identity

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.cbgm.sparrow.core.crypto.error.SignatureVerificationException
import com.ionspin.kotlin.crypto.signature.Signature

class SodiumIdentityAcknowledgementCrypto(
    private val payloadEncoder: IdentityAcknowledgementPayloadEncoder
) : IdentityAcknowledgementCrypto {
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun sign(
        acknowledgedEncryptionPublicKey: ByteArray,
        acknowledgedSigningPublicKey: ByteArray,
        senderSigningPublicKey: ByteArray,
        senderSigningPrivateKey: ByteArray
    ): Result<ByteArray> =
        runCatching {
            SodiumRuntime.initialize().getOrThrow()

            require(senderSigningPrivateKey.isNotEmpty()) {
                "Sender signing private key must not be empty"
            }

            val payload =
                payloadEncoder.encode(
                    acknowledgedEncryptionPublicKey = acknowledgedEncryptionPublicKey,
                    acknowledgedSigningPublicKey = acknowledgedSigningPublicKey,
                    senderSigningPublicKey = senderSigningPublicKey
                )

            Signature
                .detached(
                    message = payload.toUByteArray(),
                    secretKey = senderSigningPrivateKey.toUByteArray()
                ).toByteArray()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun verify(
        acknowledgedEncryptionPublicKey: ByteArray,
        acknowledgedSigningPublicKey: ByteArray,
        senderSigningPublicKey: ByteArray,
        signature: ByteArray
    ): Result<Unit> =
        runCatching {
            SodiumRuntime.initialize().getOrThrow()

            require(signature.isNotEmpty()) {
                "Acknowledgement signature must not be empty"
            }

            val payload =
                payloadEncoder.encode(
                    acknowledgedEncryptionPublicKey = acknowledgedEncryptionPublicKey,
                    acknowledgedSigningPublicKey = acknowledgedSigningPublicKey,
                    senderSigningPublicKey = senderSigningPublicKey
                )

            try {
                Signature.verifyDetached(
                    signature = signature.toUByteArray(),
                    message = payload.toUByteArray(),
                    publicKey = senderSigningPublicKey.toUByteArray()
                )
            } catch (error: Throwable) {
                throw SignatureVerificationException(error)
            }
        }
}
