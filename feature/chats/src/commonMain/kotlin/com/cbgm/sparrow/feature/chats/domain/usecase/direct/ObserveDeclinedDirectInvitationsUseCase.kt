package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.invite.domain.repository.DirectInvitationRepository
import kotlinx.coroutines.flow.Flow

class ObserveDeclinedDirectInvitationsUseCase(
    private val identityInvitationRepository: DirectInvitationRepository
) {
    operator fun invoke(): Flow<Set<String>> =
        identityInvitationRepository.observeDeclinedOutgoingContactIds()
}
