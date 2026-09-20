package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.crypto.identity.IdentityKeyGenerator
import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.core.protocol.identity.LocalSigningPublicKeyProvider
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
) : IdentityRepository,
    LocalPublicIdentityProvider,
    LocalSigningKeyPairProvider,
    LocalEncryptionKeyPairProvider,
    LocalSigningPublicKeyProvider {
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

    override suspend fun getLocalPublicIdentity(): Result<LocalPublicIdentity> =
        runCatching {
            val identity = getIdentity().getOrThrow() ?: error("Local Sparrow identity does not exist")
            LocalPublicIdentity(
                encryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                signingPublicKey = identity.signingPublicKey.copyOf()
            )
        }

    override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
        runCatching {
            val identity = getIdentity().getOrThrow() ?: error("Local Sparrow identity does not exist")
            LocalSigningKeyPair(
                publicKey = identity.signingPublicKey.copyOf(),
                privateKey = getSigningPrivateKey().getOrThrow().copyOf()
            )
        }

    override suspend fun getEncryptionKeyPair(): Result<LocalEncryptionKeyPair> =
        runCatching {
            val identity = getIdentity().getOrThrow() ?: error("Local Sparrow identity does not exist")
            val privateKey = getEncryptionPrivateKey().getOrThrow()
            require(identity.encryptionPublicKey.isNotEmpty()) { "Local encryption public key is empty" }
            require(privateKey.isNotEmpty()) { "Local encryption private key is empty" }
            LocalEncryptionKeyPair(
                publicKey = identity.encryptionPublicKey.copyOf(),
                privateKey = privateKey.copyOf()
            )
        }

    override suspend fun getSigningPublicKey(): Result<ByteArray> =
        runCatching {
            val identity = getIdentity().getOrThrow() ?: error("Local Sparrow identity does not exist")
            identity.signingPublicKey.copyOf()
        }

    private companion object {
        val IDENTITY_INTEGRITY_PAYLOAD =
            "sparrow-local-identity-integrity-v1".encodeToByteArray()
    }
}
