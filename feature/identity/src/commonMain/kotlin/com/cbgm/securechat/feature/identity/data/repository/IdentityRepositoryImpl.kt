package com.cbgm.securechat.feature.identity.data.repository

import com.cbgm.securechat.core.crypto.identity.IdentityKeyGenerator
import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.feature.identity.data.datasource.PrivateKeyStorage
import com.cbgm.securechat.feature.identity.data.datasource.PublicIdentityStorage
import com.cbgm.securechat.feature.identity.domain.model.IdentityStatus
import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class IdentityRepositoryImpl(
    private val identityKeyGenerator: IdentityKeyGenerator,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val privateKeyStorage: PrivateKeyStorage,
    private val publicIdentityStorage: PublicIdentityStorage
) : IdentityRepository {
    private val identityUpdates =
        MutableSharedFlow<PublicIdentity?>(
            replay = 1,
            extraBufferCapacity = 1
        )

    override fun observeIdentity(): Flow<PublicIdentity?> =
        flow {
            emit(publicIdentityStorage.load().getOrThrow())
            emitAll(identityUpdates)
        }

    override suspend fun getStatus(): Result<IdentityStatus> =
        runCatching {
            val publicIdentityExists = publicIdentityStorage.exists().getOrThrow()
            val privateKeysExist = privateKeyStorage.hasIdentityPrivateKeys().getOrThrow()

            when {
                !publicIdentityExists && !privateKeysExist -> IdentityStatus.NOT_CREATED
                !publicIdentityExists || !privateKeysExist -> IdentityStatus.INCOMPLETE
                !hasConsistentSigningIdentity() -> IdentityStatus.INCOMPLETE
                else -> IdentityStatus.READY
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun hasConsistentSigningIdentity(): Boolean {
        val publicIdentity = publicIdentityStorage.load().getOrNull() ?: return false
        val signingPrivateKey =
            privateKeyStorage
                .loadSigningPrivateKey()
                .getOrNull()
                ?.toByteArray()
                ?: return false
        val signature =
            signatureCrypto
                .sign(
                    payload = IDENTITY_INTEGRITY_PAYLOAD,
                    signingPrivateKey = signingPrivateKey
                ).getOrNull()
                ?: return false

        return signatureCrypto
            .verify(
                payload = IDENTITY_INTEGRITY_PAYLOAD,
                signingPublicKey = publicIdentity.signingPublicKey,
                signature = signature
            ).isSuccess
    }

    override suspend fun hasIdentity(): Result<Boolean> = getStatus().map { status -> status == IdentityStatus.READY }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun createIdentity(): Result<PublicIdentity> {
        var privateKeysWritten = false

        var publicIdentityWritten = false

        return try {
            val publicIdentityExists = publicIdentityStorage.exists().getOrThrow()

            val privateKeysExist = privateKeyStorage.hasIdentityPrivateKeys().getOrThrow()

            check(!publicIdentityExists && !privateKeysExist) {
                "Identity or partial identity state already exists"
            }

            val keyPair = identityKeyGenerator.generate().getOrThrow()

            privateKeyStorage
                .saveIdentityPrivateKeys(
                    encryptionPrivateKey = keyPair.encryptionPrivateKey,
                    signingPrivateKey = keyPair.signingPrivateKey
                ).getOrThrow()

            privateKeysWritten = true

            val publicIdentity =
                PublicIdentity(
                    encryptionPublicKey = keyPair.encryptionPublicKey.toByteArray(),
                    signingPublicKey = keyPair.signingPublicKey.toByteArray()
                )

            publicIdentityStorage.save(identity = publicIdentity).getOrThrow()

            publicIdentityWritten = true

            identityUpdates.emit(publicIdentity)

            Result.success(publicIdentity)
        } catch (
            creationError: Throwable
        ) {
            val publicRollback =
                if (publicIdentityWritten) {
                    publicIdentityStorage.delete()
                } else {
                    Result.success(Unit)
                }

            val privateRollback =
                if (privateKeysWritten) {
                    privateKeyStorage
                        .deleteIdentityPrivateKeys()
                } else {
                    Result.success(Unit)
                }

            if (publicRollback.isFailure || privateRollback.isFailure) {
                Result.failure(
                    IllegalStateException(
                        "Identity creation failed and rollback was incomplete",
                        creationError
                    )
                )
            } else {
                Result.failure(creationError)
            }
        }
    }

    override suspend fun resetIdentity(): Result<Unit> =
        runCatching {
            privateKeyStorage.deleteIdentityPrivateKeys().getOrThrow()
            publicIdentityStorage.delete().getOrThrow()
            identityUpdates.emit(null)
        }

    override suspend fun getIdentity(): Result<PublicIdentity?> = publicIdentityStorage.load()

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun getEncryptionPrivateKey(): Result<ByteArray> =
        runCatching {
            privateKeyStorage.loadEncryptionPrivateKey().getOrThrow()?.toByteArray()
                ?: error("Local encryption private key does not exist")
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun getSigningPrivateKey(): Result<ByteArray> =
        runCatching {
            privateKeyStorage.loadSigningPrivateKey().getOrThrow()?.toByteArray()
                ?: error("Local signing private key does not exist")
        }

    private companion object {
        val IDENTITY_INTEGRITY_PAYLOAD =
            "securechat-local-identity-integrity-v1".encodeToByteArray()
    }
}
