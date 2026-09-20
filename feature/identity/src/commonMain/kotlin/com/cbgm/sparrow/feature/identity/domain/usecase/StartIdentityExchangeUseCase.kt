package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchange
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class StartIdentityExchangeUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(peerId: String, invitationSenderLabel: String): Result<IdentityExchange?> =
        repository.start(peerId, invitationSenderLabel)
}
