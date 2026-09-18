package com.cbgm.sparrow.feature.identity.domain.usecase.direct

import com.cbgm.sparrow.feature.identity.domain.model.DirectInvitationRecord
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository

class StartDirectInvitationUseCase(
    private val repository: DirectIdentityExchangeRepository
) {
    suspend operator fun invoke(contactId: String): Result<DirectInvitationRecord?> =
        repository.startInvitation(contactId)
}
