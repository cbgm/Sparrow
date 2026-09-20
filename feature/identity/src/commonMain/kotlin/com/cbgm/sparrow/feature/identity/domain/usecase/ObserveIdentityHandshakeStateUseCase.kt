package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository
import kotlinx.coroutines.flow.Flow

class ObserveIdentityHandshakeStateUseCase(
    private val identityExchangeRepository: IdentityExchangeRepository
) {
    operator fun invoke(contactId: String): Flow<IdentityHandshakeState?> =
        identityExchangeRepository.observeState(contactId)
}
