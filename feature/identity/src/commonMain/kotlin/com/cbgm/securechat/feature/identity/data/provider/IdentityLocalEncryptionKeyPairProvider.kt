package com.cbgm.securechat.feature.identity.data.provider

import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository

class IdentityLocalEncryptionKeyPairProvider(
    private val identityRepository: IdentityRepository
) : LocalEncryptionKeyPairProvider {
    override suspend fun getEncryptionKeyPair(): Result<LocalEncryptionKeyPair> =
        runCatching {
            val identity =
                identityRepository
                    .getIdentity()
                    .getOrThrow()
                    ?: error(
                        "Local SecureChat identity does not exist"
                    )

            val privateKey = identityRepository.getEncryptionPrivateKey().getOrThrow()

            require(identity.encryptionPublicKey.isNotEmpty()) {
                "Local encryption public key is empty"
            }

            require(privateKey.isNotEmpty()) {
                "Local encryption private key is empty"
            }

            LocalEncryptionKeyPair(
                publicKey = identity.encryptionPublicKey.copyOf(),
                privateKey = privateKey.copyOf()
            )
        }
}
