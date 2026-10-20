package com.cbgm.sparrow.feature.identity.domain.usecase.restore

import com.cbgm.sparrow.core.result.flatMap
import com.cbgm.sparrow.core.result.safeCall
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackup
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityBackupRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository

/**
 * Decrypts a backup and checks its keys in memory only; nothing is stored. The caller owns the
 * returned private keys and must wipe them (see [RestoreIdentityBackupUseCase]).
 */
class ReadIdentityBackupUseCase(
    private val identityRepository: IdentityRepository,
    private val backupRepository: IdentityBackupRepository
) {
    suspend operator fun invoke(
        document: ByteArray,
        password: CharArray
    ): Result<IdentityBackup> = safeSuspendCall(logFailure = false) {
        identityRepository.getStatus().flatMap { status ->
            safeCall(logFailure = false) {
                check(status == IdentityStatus.NOT_CREATED) {
                    "An identity already exists. Import cannot overwrite existing keys."
                }
            }
        }.flatMap {
            backupRepository.decrypt(document, password)
        }.flatMap { backup ->
            identityRepository.validateIdentityKeys(
                backup.publicIdentity,
                backup.encryptionPrivateKey,
                backup.signingPrivateKey
            ).map { backup }.onFailure { backup.wipe() }
        }
    }.flatMap { it }
}
