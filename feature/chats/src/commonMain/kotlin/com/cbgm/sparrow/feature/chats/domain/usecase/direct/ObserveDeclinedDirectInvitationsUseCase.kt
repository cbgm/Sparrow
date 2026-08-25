package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository
import kotlinx.coroutines.flow.Flow

class ObserveDeclinedDirectInvitationsUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository
) {
    operator fun invoke(): Flow<Set<String>> =
        identityInvitationRepository.observeDeclinedOutgoingContactIds()
}
