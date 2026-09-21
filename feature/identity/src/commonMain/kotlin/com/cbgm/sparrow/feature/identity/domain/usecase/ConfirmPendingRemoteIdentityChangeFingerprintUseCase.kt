package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository

/** Records explicit out-of-band verification, not an authorization or identity rotation. */
class ConfirmPendingRemoteIdentityChangeFingerprintUseCase(
    private val repository: PendingRemoteIdentityChangeRepository
) {
    suspend operator fun invoke(
        peerId: String,
        invitationId: String,
        independentlyCheckedSigningFingerprint: String
    ): Result<Unit> = repository.confirmFingerprint(peerId, invitationId, independentlyCheckedSigningFingerprint)
}
