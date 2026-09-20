package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityPeerState
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class GetIdentityPeerStateUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(peerId: String): Result<IdentityPeerState> =
        repository.getPeerState(peerId)
}
