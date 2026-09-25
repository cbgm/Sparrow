package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.feature.identity.data.datasource.IdentityBackupStatusDataSource
import com.cbgm.sparrow.feature.identity.device.IdentityBackupCodec
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackup
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackupStatus
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityBackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IdentityBackupRepositoryImpl(
    private val codec: IdentityBackupCodec,
    private val statusDataSource: IdentityBackupStatusDataSource
) : IdentityBackupRepository {
    override suspend fun encrypt(backup: IdentityBackup, password: CharArray): Result<ByteArray> = runCatching {
        withContext(Dispatchers.Default) { codec.encrypt(backup, password) }
    }

    override suspend fun decrypt(document: ByteArray, password: CharArray): Result<IdentityBackup> = runCatching {
        withContext(Dispatchers.Default) { codec.decrypt(document, password) }
    }

    override suspend fun markExported(identity: PublicIdentity): Result<Unit> = runCatching {
        statusDataSource.set(identity.signingPublicKey + identity.encryptionPublicKey, IdentityBackupStatus.EXPORTED.name)
    }

    override suspend fun markImported(identity: PublicIdentity): Result<Unit> = runCatching {
        statusDataSource.set(identity.signingPublicKey + identity.encryptionPublicKey, IdentityBackupStatus.IMPORTED.name)
    }

    override suspend fun getStatus(identity: PublicIdentity): Result<IdentityBackupStatus> = runCatching {
        when (statusDataSource.get(identity.signingPublicKey + identity.encryptionPublicKey)) {
            IdentityBackupStatus.EXPORTED.name -> IdentityBackupStatus.EXPORTED
            IdentityBackupStatus.IMPORTED.name -> IdentityBackupStatus.IMPORTED
            else -> IdentityBackupStatus.NOT_BACKED_UP
        }
    }
}
