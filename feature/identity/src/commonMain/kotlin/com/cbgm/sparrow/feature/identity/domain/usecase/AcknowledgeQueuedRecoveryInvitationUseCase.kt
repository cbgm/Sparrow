package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.ApprovedIdentityReconnectionRepository

class AcknowledgeQueuedRecoveryInvitationUseCase(
    private val repository: ApprovedIdentityReconnectionRepository
) {
    suspend operator fun invoke(peerId: String, approvalId: String): Result<Unit> =
        repository.acknowledgeQueued(peerId, approvalId)
}
