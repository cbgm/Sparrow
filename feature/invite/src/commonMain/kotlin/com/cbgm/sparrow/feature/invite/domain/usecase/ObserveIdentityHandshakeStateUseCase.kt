package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.feature.invite.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.invite.domain.repository.DirectIdentityExchangeRepository
import kotlinx.coroutines.flow.Flow

class ObserveIdentityHandshakeStateUseCase(
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository
) {
    operator fun invoke(contactId: String): Flow<IdentityHandshakeState?> =
        directIdentityExchangeRepository.observeState(contactId)
}
