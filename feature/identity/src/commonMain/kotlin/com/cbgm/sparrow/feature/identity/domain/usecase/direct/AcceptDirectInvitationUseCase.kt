package com.cbgm.sparrow.feature.identity.domain.usecase.direct

import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class AcceptDirectInvitationUseCase(
    private val repository: DirectIdentityExchangeRepository
) {
    suspend operator fun invoke(invitationId: String): Result<Unit> =
        repository.acceptInvitation(invitationId)
}
