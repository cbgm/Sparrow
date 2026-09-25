package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackup
import com.cbgm.sparrow.feature.identity.domain.model.IdentityBackupStatus
import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.feature.identity.domain.model.PublicIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityBackupRepository
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityRepository

class PrepareIdentityBackupUseCase(
    private val identityRepository: IdentityRepository,
    private val backupRepository: IdentityBackupRepository
) {
    suspend operator fun invoke(password: CharArray): Result<ByteArray> = runCatching {
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
    suspend operator fun invoke(expectedSigningPublicKey: ByteArray, expectedEncryptionPublicKey: ByteArray): Result<Unit> = runCatching {
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
    ): Result<PublicIdentity> = runCatching {
        check(identityRepository.getStatus().getOrThrow() == IdentityStatus.NOT_CREATED) {
            "An identity already exists. Import cannot overwrite existing keys."
        }
        // Decrypt/authenticate before mutating even the local onboarding profile.
        val backup = backupRepository.decrypt(document, password).getOrThrow()
        try {
            saveLocalPhoneName(phoneNumber, name).getOrThrow()
            val restored = identityRepository.restoreIdentity(
                backup.publicIdentity,
                backup.encryptionPrivateKey,
                backup.signingPrivateKey
            ).getOrThrow()
            backupRepository.markImported(restored).getOrThrow()
            restored
        } finally {
            backup.encryptionPrivateKey.fill(0)
            backup.signingPrivateKey.fill(0)
        }
    }
}

class GetIdentityBackupStatusUseCase(
    private val identityRepository: IdentityRepository,
    private val backupRepository: IdentityBackupRepository
) {
    suspend operator fun invoke(): Result<IdentityBackupStatus> = runCatching {
        val identity = identityRepository.getIdentity().getOrThrow() ?: return@runCatching IdentityBackupStatus.NOT_BACKED_UP
        backupRepository.getStatus(identity).getOrThrow()
    }
}
