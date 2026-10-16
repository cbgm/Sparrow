package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackup
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackupState
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityBackupRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository
import kotlin.getOrThrow

class PrepareIdentityBackupUseCase(
    private val identityRepository: IdentityRepository,
    private val backupRepository: IdentityBackupRepository
) {
    suspend operator fun invoke(
        password: CharArray
    ): Result<ByteArray> = safeSuspendCall(logFailure = false) {
        require(password.size >= 12) { "Use an identity backup password with at least 12 characters" }
        check(identityRepository.getStatus().getOrThrow() == IdentityStatus.READY) { "Identity is not ready" }
        val public = requireNotNull(identityRepository.getIdentity().getOrThrow())
        val encryption = identityRepository.getEncryptionPrivateKey().getOrThrow()
        val signing = identityRepository.getSigningPrivateKey().getOrThrow()
        try {
            backupRepository.encrypt(IdentityBackup(public, encryption, signing), password).getOrThrow()
        } finally {
            encryption.fill(0)
            signing.fill(0)
        }
    }
}

class MarkIdentityBackupExportedUseCase(
    private val identityRepository: IdentityRepository,
    private val backupRepository: IdentityBackupRepository
) {
    /** Called only after SAF has successfully closed the written backup document. */
    suspend operator fun invoke(
        expectedSigningPublicKey: ByteArray,
        expectedEncryptionPublicKey: ByteArray
    ): Result<Unit> = safeSuspendCall(logFailure = false) {
        check(identityRepository.getStatus().getOrThrow() == IdentityStatus.READY)
        val current = requireNotNull(identityRepository.getIdentity().getOrThrow())
        check(
            current.signingPublicKey.contentEquals(expectedSigningPublicKey) &&
                current.encryptionPublicKey.contentEquals(expectedEncryptionPublicKey)
        ) {
            "The identity changed during backup export"
        }
        backupRepository.markExported(current).getOrThrow()
    }
}

class GetIdentityBackupStatusUseCase(
    private val identityRepository: IdentityRepository,
    private val backupRepository: IdentityBackupRepository
) {
    suspend operator fun invoke(): Result<IdentityBackupState> = safeSuspendCall(logFailure = false) {
        val identity =
            identityRepository.getIdentity().getOrThrow()
                ?: return@safeSuspendCall IdentityBackupState.NOT_BACKED_UP
        backupRepository.getStatus(identity).getOrThrow()
    }
}
