package com.cbgm.sparrow.feature.identity.device

enum class PrivateKeyState {
    /** No private-key entries are stored. */
    ABSENT,

    /** Only some entries are stored, as after an interrupted setup. */
    PARTIAL,

    /** All entries are stored and decrypt. */
    AVAILABLE,

    /** The entries are stored but can never be decrypted again: the key-store key is gone or invalidated, or the data is damaged. */
    UNUSABLE
}

interface PrivateKeyStorage {
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun saveIdentityPrivateKeys(
        encryptionPrivateKey: UByteArray,
        signingPrivateKey: UByteArray
    )

    /** True even when some or all persisted private-key entries are damaged or undecryptable. */
    suspend fun hasAnyIdentityPrivateKeyMaterial(): Boolean

    /**
     * Whether the stored private keys can be used. Throws when this cannot be decided right now (for example
     * a busy key store); only [PrivateKeyState.UNUSABLE] means the keys are lost for good.
     */
    suspend fun privateKeyState(): PrivateKeyState

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun loadEncryptionPrivateKey(): UByteArray?

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun loadSigningPrivateKey(): UByteArray?

    suspend fun deleteIdentityPrivateKeys()

    /** Developer tool: removes only the key-store key, so the stored private keys can no longer be decrypted. */
    suspend fun discardWrappingKey()
}
