package com.cbgm.sparrow.feature.identity.domain.usecase.restore

import com.cbgm.sparrow.core.result.flatMap
import com.cbgm.sparrow.core.result.safeCall
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityBackupRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.SaveLocalPhoneNameUseCase

class RestoreIdentityBackupUseCase(
    private val identityRepository: IdentityRepository,
    private val backupRepository: IdentityBackupRepository,
    private val saveLocalPhoneName: SaveLocalPhoneNameUseCase
) {
    suspend operator fun invoke(
        document: ByteArray,
        password: CharArray,
        phoneNumber: String,
        name: String
    ): Result<PublicIdentity> = safeSuspendCall(logFailure = false) {
        identityRepository.getStatus().flatMap { status ->
            safeCall(logFailure = false) {
                check(status == IdentityStatus.NOT_CREATED) {
                    "An identity already exists. Import cannot overwrite existing keys."
                }
            }
        }.flatMap {
            backupRepository.decrypt(document, password)
        }.flatMap { backup ->
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
                backup.encryptionPrivateKey.fill(0)
                backup.signingPrivateKey.fill(0)
            }
        }
    }.flatMap { it }
}
