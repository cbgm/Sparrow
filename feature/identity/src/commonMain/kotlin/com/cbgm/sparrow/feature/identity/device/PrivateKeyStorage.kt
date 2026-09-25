package com.cbgm.sparrow.feature.identity.device

interface PrivateKeyStorage {
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun saveIdentityPrivateKeys(
        encryptionPrivateKey: UByteArray,
        signingPrivateKey: UByteArray
    )

    /** True even when some or all persisted private-key entries are damaged or undecryptable. */
    suspend fun hasAnyIdentityPrivateKeyMaterial(): Boolean

    suspend fun hasIdentityPrivateKeys(): Boolean

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun loadEncryptionPrivateKey(): UByteArray?

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun loadSigningPrivateKey(): UByteArray?

    suspend fun deleteIdentityPrivateKeys()
}
