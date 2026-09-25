package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.PendingRemoteIdentityChangeRepository

/** Dismisses local review only. Does NOT reject the contact on the wire or authorize new keys. */
class DismissPendingRemoteIdentityChangeUseCase(
    private val repository: PendingRemoteIdentityChangeRepository
) {
    suspend operator fun invoke(peerId: String, invitationId: String): Result<Unit> =
        repository.discard(peerId, invitationId)
}
