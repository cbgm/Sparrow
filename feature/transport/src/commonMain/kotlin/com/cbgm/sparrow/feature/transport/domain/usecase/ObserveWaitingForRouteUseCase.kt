package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.port.DirectSendingPort
import kotlinx.coroutines.flow.Flow

/** True while messages wait: sending directly is switched off and no circuit through enough hops is up. */
class ObserveWaitingForRouteUseCase(
    private val sendingPolicy: DirectSendingPort
) {
    operator fun invoke(): Flow<Boolean> = sendingPolicy.observeWaiting()
}
