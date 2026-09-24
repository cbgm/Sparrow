package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository

/** Explicit, one-tap approval of the exact proposed keys visible in the Mailbox. */
class ApprovePendingRemoteIdentityChangeUseCase(
    private val repository: PendingRemoteIdentityChangeRepository
) {
    suspend operator fun invoke(
        peerId: String,
        invitationId: String,
        presentedSigningFingerprint: String,
        presentedEncryptionFingerprint: String
    ): Result<Unit> = repository.approveReplacement(
        peerId,
        invitationId,
        presentedSigningFingerprint,
        presentedEncryptionFingerprint
    )
}
