package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

class DiscardSupersededMembershipsUseCase(
    private val repository: MembershipRepository
) {
    suspend operator fun invoke(
        peerId: String,
        currentSourceId: String
    ): Result<Unit> =
        repository.discardSupersededIncomingHandshakes(peerId, currentSourceId)
}
