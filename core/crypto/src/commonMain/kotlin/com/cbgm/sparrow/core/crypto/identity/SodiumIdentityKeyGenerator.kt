package com.cbgm.sparrow.core.crypto.identity

import com.cbgm.sparrow.core.crypto.SodiumRuntime
import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.signature.Signature

class SodiumIdentityKeyGenerator : IdentityKeyGenerator {
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun generate(): Result<IdentityKeyPair> =
        runCatching {
            SodiumRuntime.initialize().getOrThrow()

            val encryptionKeyPair = Box.keypair()

            val signingKeyPair = Signature.keypair()

            IdentityKeyPair(
                encryptionPublicKey = encryptionKeyPair.publicKey,
                encryptionPrivateKey = encryptionKeyPair.secretKey,
                signingPublicKey = signingKeyPair.publicKey,
                signingPrivateKey = signingKeyPair.secretKey
            )
        }
}
