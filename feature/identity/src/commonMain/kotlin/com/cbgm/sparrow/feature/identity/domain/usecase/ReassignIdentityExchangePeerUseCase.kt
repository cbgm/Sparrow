package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class ReassignIdentityExchangePeerUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(
        fromPeerId: String,
        toPeerId: String
    ): Result<Unit> = repository.reassignPeer(fromPeerId, toPeerId)
}
