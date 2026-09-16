package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.invite.domain.repository.IdentityInvitationRepository
import kotlinx.coroutines.flow.Flow

class ObserveIdentityHandshakeStateUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository
) {
    operator fun invoke(contactId: String): Flow<IdentityHandshakeState?> =
        identityInvitationRepository.observeState(contactId)
}
