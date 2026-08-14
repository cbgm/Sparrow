package com.cbgm.securechat.feature.identity.data.datasource

interface PrivateKeyStorage {
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun saveIdentityPrivateKeys(
        encryptionPrivateKey: UByteArray,
        signingPrivateKey: UByteArray
    ): Result<Unit>

    suspend fun hasIdentityPrivateKeys(): Result<Boolean>

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun loadEncryptionPrivateKey(): Result<UByteArray?>

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun loadSigningPrivateKey(): Result<UByteArray?>

    /**
     * Deletes all locally stored encrypted identity private-key blobs.
     *
     * Used when:
     * - identity creation must be rolled back
     * - the user explicitly resets their identity later
     */
    suspend fun deleteIdentityPrivateKeys(): Result<Unit>
}
