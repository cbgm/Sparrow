package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository
import kotlinx.coroutines.flow.Flow

class ObserveIdentityHandshakeStateUseCase(
    private val identityInvitationRepository: DirectInvitationRepository
) {
    operator fun invoke(contactId: String): Flow<IdentityHandshakeState?> =
        identityInvitationRepository.observeState(contactId)
}
