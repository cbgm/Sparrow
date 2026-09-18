package com.cbgm.sparrow.feature.identity.domain.usecase.direct

import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class DeclineDirectInvitationUseCase(
    private val repository: DirectIdentityExchangeRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        repository.declineInvitation(invitationId)
}
