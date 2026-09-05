package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.crypto.identity.IdentityKeyGenerator
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.data.datasource.PublicIdentityDataSource
import com.cbgm.sparrow.feature.identity.device.PrivateKeyStorage
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class IdentityRepositoryImpl(
    private val identityKeyGenerator: IdentityKeyGenerator,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val privateKeyStorage: PrivateKeyStorage,
    private val publicIdentityDataSource: PublicIdentityDataSource
) : IdentityRepository {
    private val identityUpdates =
        MutableSharedFlow<PublicIdentity?>(
            replay = 1,
            extraBufferCapacity = 1
        )

    override fun observeIdentity(): Flow<PublicIdentity?> =
        flow {
            emit(publicIdentityDataSource.load())
            emitAll(identityUpdates)
        }

    override suspend fun getStatus(): Result<IdentityStatus> =
        safeSuspendCall {
            val publicIdentityExists = publicIdentityDataSource.exists()
            val privateKeysExist = privateKeyStorage.hasIdentityPrivateKeys()

            when {
                !publicIdentityExists && !privateKeysExist -> IdentityStatus.NOT_CREATED
                !publicIdentityExists || !privateKeysExist -> IdentityStatus.INCOMPLETE
                !hasConsistentSigningIdentity() -> IdentityStatus.INCOMPLETE
                else -> IdentityStatus.READY
            }
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun hasConsistentSigningIdentity(): Boolean {
        val publicIdentity = publicIdentityDataSource.load() ?: return false
        val signingPrivateKey = privateKeyStorage.loadSigningPrivateKey()?.toByteArray() ?: return false
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

    override suspend fun hasIdentity(): Result<Boolean> =
        getStatus().map { status -> status == IdentityStatus.READY }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun createIdentity(): Result<PublicIdentity> =
        safeSuspendCall {
            var privateKeysWritten = false
            var publicIdentityWritten = false

            try {
                val publicIdentityExists = publicIdentityDataSource.exists()
                val privateKeysExist = privateKeyStorage.hasIdentityPrivateKeys()

                check(!publicIdentityExists && !privateKeysExist) {
                    "Identity or partial identity state already exists"
                }

                val keyPair = identityKeyGenerator.generate().getOrThrow()

                privateKeyStorage.saveIdentityPrivateKeys(
                    encryptionPrivateKey = keyPair.encryptionPrivateKey,
                    signingPrivateKey = keyPair.signingPrivateKey
                )
                privateKeysWritten = true

                val publicIdentity =
                    PublicIdentity(
                        encryptionPublicKey = keyPair.encryptionPublicKey.toByteArray(),
                        signingPublicKey = keyPair.signingPublicKey.toByteArray()
                    )

                publicIdentityDataSource.save(identity = publicIdentity)
                publicIdentityWritten = true
                identityUpdates.emit(publicIdentity)
                publicIdentity
            } catch (creationError: Throwable) {
                val publicRollback =
                    safeSuspendCall {
                        if (publicIdentityWritten) {
                            publicIdentityDataSource.delete()
                        }
                    }
                val privateRollback =
                    safeSuspendCall {
                        if (privateKeysWritten) {
                            privateKeyStorage.deleteIdentityPrivateKeys()
                        }
                    }

                if (publicRollback.isFailure || privateRollback.isFailure) {
                    throw IllegalStateException(
                        "Identity creation failed and rollback was incomplete",
                        creationError
                    )
                }
                throw creationError
            }
        }

    override suspend fun resetIdentity(): Result<Unit> =
        safeSuspendCall {
            privateKeyStorage.deleteIdentityPrivateKeys()
            publicIdentityDataSource.delete()
            identityUpdates.emit(null)
        }

    override suspend fun getIdentity(): Result<PublicIdentity?> =
        safeSuspendCall { publicIdentityDataSource.load() }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun getEncryptionPrivateKey(): Result<ByteArray> =
        safeSuspendCall {
            privateKeyStorage.loadEncryptionPrivateKey()?.toByteArray()
                ?: error("Local encryption private key does not exist")
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun getSigningPrivateKey(): Result<ByteArray> =
        safeSuspendCall {
            privateKeyStorage.loadSigningPrivateKey()?.toByteArray()
                ?: error("Local signing private key does not exist")
        }

    private companion object {
        val IDENTITY_INTEGRITY_PAYLOAD =
            "sparrow-local-identity-integrity-v1".encodeToByteArray()
    }
}
