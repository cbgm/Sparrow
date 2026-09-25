package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeReady
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class ReceiveIdentityReadyUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(context: IncomingPacketContext, ready: IdentityExchangeReady): Result<Unit> =
        repository.receiveReady(context, ready)
}
