package com.cbgm.sparrow.feature.chats.domain.usecase.direct

import com.cbgm.sparrow.feature.invite.domain.repository.DirectIdentityExchangeRepository
import kotlinx.coroutines.flow.Flow

class ObserveDeclinedDirectInvitationsUseCase(
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository
) {
    operator fun invoke(): Flow<Set<String>> =
        directIdentityExchangeRepository.observeDeclinedOutgoingContactIds()
}
