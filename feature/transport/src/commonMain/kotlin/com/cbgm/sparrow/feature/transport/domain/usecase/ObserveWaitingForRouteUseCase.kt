package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.circuit.DirectSendingPolicy
import kotlinx.coroutines.flow.Flow

/** True while messages wait: sending directly is switched off and no circuit through enough hops is up. */
class ObserveWaitingForRouteUseCase(
    private val sendingPolicy: DirectSendingPolicy
) {
    operator fun invoke(): Flow<Boolean> = sendingPolicy.observeWaiting()
}
