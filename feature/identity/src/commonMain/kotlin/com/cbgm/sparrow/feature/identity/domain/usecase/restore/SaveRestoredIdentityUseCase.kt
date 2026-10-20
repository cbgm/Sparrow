package com.cbgm.sparrow.feature.identity.domain.usecase.restore

import com.cbgm.sparrow.core.result.flatMap
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackup
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityBackupRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.SaveLocalPhoneNameUseCase

/** Stores a backup read by [ReadIdentityBackupUseCase] after the phone number was verified. */
class SaveRestoredIdentityUseCase(
    private val identityRepository: IdentityRepository,
    private val backupRepository: IdentityBackupRepository,
    private val saveLocalPhoneName: SaveLocalPhoneNameUseCase
) {
    suspend operator fun invoke(
        backup: IdentityBackup,
        phoneNumber: String,
        name: String
    ): Result<PublicIdentity> = safeSuspendCall(logFailure = false) {
        try {
            saveLocalPhoneName(phoneNumber, name).flatMap {
                identityRepository.restoreIdentity(
                    backup.publicIdentity,
                    backup.encryptionPrivateKey,
                    backup.signingPrivateKey
                )
            }.flatMap { restored ->
                backupRepository.markImported(restored).map { restored }
            }
        } finally {
            backup.wipe()
        }
    }.flatMap { it }
}
