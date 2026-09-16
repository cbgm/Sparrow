package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository
import kotlinx.coroutines.flow.Flow

class ObserveIdentityHandshakeStateUseCase(
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository
) {
    operator fun invoke(contactId: String): Flow<IdentityHandshakeState?> =
        directIdentityExchangeRepository.observeState(contactId)
}
