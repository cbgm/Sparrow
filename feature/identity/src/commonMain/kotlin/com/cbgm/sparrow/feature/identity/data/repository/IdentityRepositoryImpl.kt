package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.sparrow.core.crypto.transport.TransportMessageCipher
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.crypto.IdentityKeyGenerator
import com.cbgm.sparrow.feature.identity.data.datasource.PublicIdentityDataSource
import com.cbgm.sparrow.feature.identity.device.PrivateKeyState
import com.cbgm.sparrow.feature.identity.device.PrivateKeyStorage
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackupMismatchException
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.sparrow.protocol.identity.LocalIdentityUnavailableException
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.sparrow.protocol.identity.LocalSigningPublicKeyProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class IdentityRepositoryImpl(
    private val identityKeyGenerator: IdentityKeyGenerator,
    private val signatureCrypto: DetachedSignatureCrypto,
    private val transportCipher: TransportMessageCipher,
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

    // False once the stored identity's private keys turned out to be gone while the app runs.
    private val keysUsable = MutableStateFlow(true)

    override fun observeKeysUsable(): Flow<Boolean> = keysUsable

    override fun observeIdentity(): Flow<PublicIdentity?> =
        flow {
            emit(publicIdentityDataSource.load())
            emitAll(identityUpdates)
        }

    override suspend fun getStatus(): Result<IdentityStatus> =
        safeSuspendCall {
            val publicIdentityExists = publicIdentityDataSource.exists()
            // Throws when the keys cannot be read right now; that is a failure, never INCOMPLETE.
            val privateKeyState = privateKeyStorage.privateKeyState()

            when {
                !publicIdentityExists && privateKeyState == PrivateKeyState.ABSENT -> IdentityStatus.NOT_CREATED
                !publicIdentityExists || privateKeyState != PrivateKeyState.AVAILABLE -> IdentityStatus.INCOMPLETE
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
                val privateKeysExist = privateKeyStorage.hasAnyIdentityPrivateKeyMaterial()

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

    override suspend fun validateIdentityKeys(
        publicIdentity: PublicIdentity,
        encryptionPrivateKey: ByteArray,
        signingPrivateKey: ByteArray
    ): Result<Unit> = safeSuspendCall(logFailure = false) {
        require(
            publicIdentity.encryptionPublicKey.size == 32 && encryptionPrivateKey.size == 32 &&
                publicIdentity.signingPublicKey.size == 32 && signingPrivateKey.size == 64
        ) {
            "Invalid identity key lengths"
        }
        val testPayload = "sparrow-backup-key-validation-v1".encodeToByteArray()
        val signature = signatureCrypto.sign(testPayload, signingPrivateKey).getOrThrow()
        signatureCrypto.verify(testPayload, publicIdentity.signingPublicKey, signature).getOrThrow()
        val ciphertext = transportCipher.encryptForRecipient(testPayload, publicIdentity.encryptionPublicKey).getOrThrow()
        val decrypted = transportCipher.decryptFromSender(
            ciphertext,
            publicIdentity.encryptionPublicKey,
            encryptionPrivateKey
        ).getOrThrow()
        check(decrypted.contentEquals(testPayload)) { "Backup encryption key does not match public identity" }
    }

    /** Validates both key pairs BEFORE persisting either; fails closed if any identity state exists. */
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun restoreIdentity(
        publicIdentity: PublicIdentity,
        encryptionPrivateKey: ByteArray,
        signingPrivateKey: ByteArray
    ): Result<PublicIdentity> = safeSuspendCall {
        check(!publicIdentityDataSource.exists() && !privateKeyStorage.hasAnyIdentityPrivateKeyMaterial()) {
            "An identity or partial identity already exists"
        }
        validateIdentityKeys(publicIdentity, encryptionPrivateKey, signingPrivateKey).getOrThrow()
        // The private-key storage uses one DataStore edit for both already-wrapped private keys.
        // Public keys are stored only afterwards; a failed import attempts rollback of both.
        try {
            privateKeyStorage.saveIdentityPrivateKeys(
                encryptionPrivateKey.toUByteArray(),
                signingPrivateKey.toUByteArray()
            )
            publicIdentityDataSource.save(publicIdentity)
        } catch (error: Throwable) {
            val rollback = safeSuspendCall {
                publicIdentityDataSource.delete()
                privateKeyStorage.deleteIdentityPrivateKeys()
            }
            if (rollback.isFailure) throw IllegalStateException("Identity import rollback failed", error)
            throw error
        }
        identityUpdates.emit(publicIdentity)
        publicIdentity
    }

    /**
     * Puts the private keys of the stored identity back from a backup. Only a backup of the same identity is
     * accepted, so chats and contacts stay valid; the old, unusable key-store key is replaced.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun repairIdentityKeys(
        publicIdentity: PublicIdentity,
        encryptionPrivateKey: ByteArray,
        signingPrivateKey: ByteArray
    ): Result<PublicIdentity> = safeSuspendCall {
        val stored = publicIdentityDataSource.load() ?: throw IdentityBackupMismatchException()
        if (stored != publicIdentity) throw IdentityBackupMismatchException()
        validateIdentityKeys(publicIdentity, encryptionPrivateKey, signingPrivateKey).getOrThrow()
        privateKeyStorage.deleteIdentityPrivateKeys()
        privateKeyStorage.saveIdentityPrivateKeys(encryptionPrivateKey.toUByteArray(), signingPrivateKey.toUByteArray())
        keysUsable.value = true
        identityUpdates.emit(stored)
        stored
    }

    override suspend fun discardKeyStoreKey(): Result<Unit> =
        safeSuspendCall {
            privateKeyStorage.discardWrappingKey()
            keysUsable.value = false
        }

    override suspend fun resetIdentity(): Result<Unit> =
        safeSuspendCall {
            privateKeyStorage.deleteIdentityPrivateKeys()
            publicIdentityDataSource.delete()
            keysUsable.value = true
            identityUpdates.emit(null)
        }

    override suspend fun getIdentity(): Result<PublicIdentity?> =
        safeSuspendCall { publicIdentityDataSource.load() }

    // Missing or lost keys are an identity state (startup offers restore or reset), not an error to report.
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun getEncryptionPrivateKey(): Result<ByteArray> =
        safeSuspendCall(expectedFailure = { error -> error is LocalIdentityUnavailableException }) {
            privateKeyStorage.loadEncryptionPrivateKey()?.toByteArray() ?: throw keysUnavailable()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun getSigningPrivateKey(): Result<ByteArray> =
        safeSuspendCall(expectedFailure = { error -> error is LocalIdentityUnavailableException }) {
            privateKeyStorage.loadSigningPrivateKey()?.toByteArray() ?: throw keysUnavailable()
        }

    private suspend fun keysUnavailable(): LocalIdentityUnavailableException {
        if (publicIdentityDataSource.exists()) keysUsable.value = false
        return LocalIdentityUnavailableException()
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
            // FCM may ask for a routing ID before onboarding has created the identity.
            // Use the typed absence signal so PushTokenRegistrationWorker can defer
            // registration without publishing an expected startup state as a global error.
            val identity = getIdentity().getOrThrow() ?: throw LocalIdentityUnavailableException()
            identity.signingPublicKey.copyOf()
        }

    private companion object {
        val IDENTITY_INTEGRITY_PAYLOAD =
            "sparrow-local-identity-integrity-v1".encodeToByteArray()
    }
}
