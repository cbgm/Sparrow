package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class CancelIdentityExchangeUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(peerId: String): Result<Unit> = repository.cancel(peerId)
}
