package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeClosure
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class GetIdentityExchangeClosureUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(peerId: String): Result<IdentityExchangeClosure?> =
        repository.getExchangeClosure(peerId)
}
