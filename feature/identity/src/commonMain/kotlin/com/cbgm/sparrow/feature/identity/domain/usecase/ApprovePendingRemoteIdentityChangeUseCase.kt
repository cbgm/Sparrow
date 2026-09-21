package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository

/** Explicit remote-key cutover; DOES NOT accept a chat invitation or establish mutual trust. */
class ApprovePendingRemoteIdentityChangeUseCase(
    private val repository: PendingRemoteIdentityChangeRepository
) {
    suspend operator fun invoke(peerId: String, invitationId: String): Result<Unit> =
        repository.approveReplacement(peerId, invitationId)
}
