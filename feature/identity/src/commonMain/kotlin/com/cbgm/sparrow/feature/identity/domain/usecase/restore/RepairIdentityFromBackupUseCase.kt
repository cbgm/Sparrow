package com.cbgm.sparrow.feature.identity.domain.usecase.restore

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityBackupRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository

/**
 * Puts back the private keys of the identity on this device from its backup, keeping chats and contacts.
 * A backup of another identity fails with IdentityBackupMismatchException.
 */
class RepairIdentityFromBackupUseCase(
    private val identityRepository: IdentityRepository,
    private val backupRepository: IdentityBackupRepository
) {
    suspend operator fun invoke(
        document: ByteArray,
        password: CharArray
    ): Result<PublicIdentity> =
        safeSuspendCall(logFailure = false) {
            val backup = backupRepository.decrypt(document, password).getOrThrow()
            try {
                identityRepository
                    .repairIdentityKeys(backup.publicIdentity, backup.encryptionPrivateKey, backup.signingPrivateKey)
                    .getOrThrow()
            } finally {
                backup.wipe()
            }
        }
}
