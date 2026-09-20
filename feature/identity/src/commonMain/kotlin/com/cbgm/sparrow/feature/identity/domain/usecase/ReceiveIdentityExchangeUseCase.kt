package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.feature.identity.domain.model.IdentityExchangeOffer
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityExchangeRepository

class ReceiveIdentityExchangeUseCase(
    private val repository: IdentityExchangeRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        offer: IdentityExchangeOffer,
        wasKnownPeerAtReceive: Boolean
    ): Result<Unit> = repository.receiveExchange(context, offer, wasKnownPeerAtReceive)
}
