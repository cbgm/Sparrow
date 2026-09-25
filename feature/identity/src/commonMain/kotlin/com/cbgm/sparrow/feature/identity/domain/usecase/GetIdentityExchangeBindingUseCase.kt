package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeBinding
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class GetIdentityExchangeBindingUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(exchangeId: String): Result<IdentityExchangeBinding?> =
        repository.getExchangeBinding(exchangeId)
}
