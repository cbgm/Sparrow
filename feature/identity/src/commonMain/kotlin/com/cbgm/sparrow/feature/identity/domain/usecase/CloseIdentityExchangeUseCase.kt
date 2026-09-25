package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class CloseIdentityExchangeUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(exchangeId: String, peerId: String): Result<Unit> =
        repository.closeExchange(exchangeId, peerId)
}
