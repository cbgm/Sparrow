package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackup
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackupStatus
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity

/** Owns backup cryptography and fingerprint-scoped export status; does not orchestrate identity storage. */
interface IdentityBackupRepository {
    suspend fun encrypt(backup: IdentityBackup, password: CharArray): Result<ByteArray>

    suspend fun decrypt(document: ByteArray, password: CharArray): Result<IdentityBackup>

    suspend fun markExported(identity: PublicIdentity): Result<Unit>

    suspend fun markImported(identity: PublicIdentity): Result<Unit>

    suspend fun getStatus(identity: PublicIdentity): Result<IdentityBackupStatus>
}
