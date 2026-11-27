package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import kotlinx.coroutines.flow.Flow

interface IdentityRepository {
    fun observeIdentity(): Flow<PublicIdentity?>

    /** Turns false when the stored identity's private keys are found to be gone while the app runs. */
    fun observeKeysUsable(): Flow<Boolean>

    suspend fun getStatus(): Result<IdentityStatus>

    suspend fun hasIdentity(): Result<Boolean>

    suspend fun createIdentity(): Result<PublicIdentity>

    /** Checks that the private keys belong to [publicIdentity]; nothing is stored. */
    suspend fun validateIdentityKeys(
        publicIdentity: PublicIdentity,
        encryptionPrivateKey: ByteArray,
        signingPrivateKey: ByteArray
    ): Result<Unit>

    suspend fun restoreIdentity(
        publicIdentity: PublicIdentity,
        encryptionPrivateKey: ByteArray,
        signingPrivateKey: ByteArray
    ): Result<PublicIdentity>

    /** Stores the private keys of a backup for the identity already on this device; fails for any other identity. */
    suspend fun repairIdentityKeys(
        publicIdentity: PublicIdentity,
        encryptionPrivateKey: ByteArray,
        signingPrivateKey: ByteArray
    ): Result<PublicIdentity>

    suspend fun resetIdentity(): Result<Unit>

    /** Developer tool to test lost keys: keeps everything except the key-store key. */
    suspend fun discardKeyStoreKey(): Result<Unit>

    suspend fun getIdentity(): Result<PublicIdentity?>

    suspend fun getEncryptionPrivateKey(): Result<ByteArray>

    suspend fun getSigningPrivateKey(): Result<ByteArray>
}
