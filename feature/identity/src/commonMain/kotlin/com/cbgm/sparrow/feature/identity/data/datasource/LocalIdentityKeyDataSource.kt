package com.cbgm.sparrow.feature.identity.data.datasource

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.device.PrivateKeyStorage
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.sparrow.protocol.identity.LocalEncryptionKeyPairSource
import com.cbgm.sparrow.protocol.identity.LocalIdentityUnavailableException
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentitySource
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.protocol.identity.LocalSigningKeyPairSource
import com.cbgm.sparrow.protocol.identity.LocalSigningPublicKeySource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** The keys of this device's identity, for the identity repository and for every other module that signs or decrypts. */
class LocalIdentityKeyDataSource(
    private val privateKeyStorage: PrivateKeyStorage,
    private val publicIdentityDataSource: PublicIdentityDataSource
) : LocalPublicIdentitySource,
    LocalSigningKeyPairSource,
    LocalEncryptionKeyPairSource,
    LocalSigningPublicKeySource {
    // False once the stored identity's private keys turned out to be gone while the app runs.
    private val keysUsable = MutableStateFlow(true)

    fun observeKeysUsable(): Flow<Boolean> = keysUsable

    fun setKeysUsable(usable: Boolean) {
        keysUsable.value = usable
    }

    // Missing or lost keys are an identity state (startup offers restore or reset), not an error to report.
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun getEncryptionPrivateKey(): Result<ByteArray> =
        safeSuspendCall(expectedFailure = { error -> error is LocalIdentityUnavailableException }) {
            privateKeyStorage.loadEncryptionPrivateKey()?.toByteArray() ?: throw keysUnavailable()
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun getSigningPrivateKey(): Result<ByteArray> =
        safeSuspendCall(expectedFailure = { error -> error is LocalIdentityUnavailableException }) {
            privateKeyStorage.loadSigningPrivateKey()?.toByteArray() ?: throw keysUnavailable()
        }

    override suspend fun getLocalPublicIdentity(): Result<LocalPublicIdentity> =
        runCatching {
            val identity = publicIdentityDataSource.load() ?: error("Local Sparrow identity does not exist")
            LocalPublicIdentity(
                encryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                signingPublicKey = identity.signingPublicKey.copyOf()
            )
        }

    override suspend fun getSigningKeyPair(): Result<LocalSigningKeyPair> =
        runCatching {
            val identity = publicIdentityDataSource.load() ?: error("Local Sparrow identity does not exist")
            LocalSigningKeyPair(
                publicKey = identity.signingPublicKey.copyOf(),
                privateKey = getSigningPrivateKey().getOrThrow().copyOf()
            )
        }

    override suspend fun getEncryptionKeyPair(): Result<LocalEncryptionKeyPair> =
        runCatching {
            val identity = publicIdentityDataSource.load() ?: error("Local Sparrow identity does not exist")
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
            // Background work may ask before onboarding has created the identity. The typed absence
            // signal lets it wait without publishing an expected startup state as a global error.
            val identity = publicIdentityDataSource.load() ?: throw LocalIdentityUnavailableException()
            identity.signingPublicKey.copyOf()
        }

    private suspend fun keysUnavailable(): LocalIdentityUnavailableException {
        if (publicIdentityDataSource.exists()) keysUsable.value = false
        return LocalIdentityUnavailableException()
    }
}
