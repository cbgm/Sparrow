package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class AcceptIdentityExchangeUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(exchangeId: String): Result<Unit> = repository.accept(exchangeId)
}
