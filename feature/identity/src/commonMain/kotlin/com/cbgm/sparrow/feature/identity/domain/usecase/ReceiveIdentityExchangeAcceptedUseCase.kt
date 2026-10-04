package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeAcceptance
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext

class ReceiveIdentityExchangeAcceptedUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(context: IncomingPacketContext, acceptance: IdentityExchangeAcceptance): Result<Unit> =
        repository.receiveAccepted(context, acceptance)
}
