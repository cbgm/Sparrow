package com.cbgm.sparrow.core.crypto.signature

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.ionspin.kotlin.crypto.signature.Signature

class SodiumDetachedSignatureCrypto : DetachedSignatureCrypto {
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun sign(
        payload: ByteArray,
        signingPrivateKey: ByteArray
    ): Result<ByteArray> =
        runCatching {
            require(payload.isNotEmpty()) {
                "Signature payload must not be empty"
            }
            require(signingPrivateKey.isNotEmpty()) {
                "Signing private key must not be empty"
            }

            SodiumRuntime.initialize().getOrThrow()

            Signature
                .detached(
                    message = payload.toUByteArray(),
                    secretKey = signingPrivateKey.toUByteArray()
                ).toByteArray()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun verify(
        payload: ByteArray,
        signingPublicKey: ByteArray,
        signature: ByteArray
    ): Result<Unit> =
        runCatching {
            require(payload.isNotEmpty()) {
                "Signature payload must not be empty"
            }
            require(signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }
            require(signature.isNotEmpty()) {
                "Signature must not be empty"
            }

            SodiumRuntime.initialize().getOrThrow()

            Signature.verifyDetached(
                signature = signature.toUByteArray(),
                message = payload.toUByteArray(),
                publicKey = signingPublicKey.toUByteArray()
            )
        }
}
