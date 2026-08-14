package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import kotlinx.coroutines.flow.Flow

class ObserveIdentityHandshakeStateUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository
) {
    operator fun invoke(contactId: String): Flow<IdentityHandshakeState?> =
        identityInvitationRepository.observeState(contactId)
}
