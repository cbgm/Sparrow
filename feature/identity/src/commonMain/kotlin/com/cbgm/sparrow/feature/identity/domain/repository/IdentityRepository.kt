package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import kotlinx.coroutines.flow.Flow

interface IdentityRepository {
    fun observeIdentity(): Flow<PublicIdentity?>

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

    suspend fun resetIdentity(): Result<Unit>

    suspend fun getIdentity(): Result<PublicIdentity?>

    suspend fun getEncryptionPrivateKey(): Result<ByteArray>

    suspend fun getSigningPrivateKey(): Result<ByteArray>
}
