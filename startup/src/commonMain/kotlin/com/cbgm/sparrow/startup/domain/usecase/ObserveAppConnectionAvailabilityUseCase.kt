package com.cbgm.sparrow.startup.domain.usecase

import com.cbgm.sparrow.feature.transport.connection.TransportConnectionManager
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.startup.domain.model.AppConnectionAvailability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull

class ObserveAppConnectionAvailabilityUseCase(
    private val transportConnectionManager: TransportConnectionManager
) {
    operator fun invoke(): Flow<AppConnectionAvailability> =
        transportConnectionManager
            .connectionState
            // Only a failed attempt counts as unavailable. Connecting and Disconnected (e.g. while
            // the app is in the background) keep the last value, so no offline hint shows early.
            .mapNotNull { state ->
                when (state) {
                    is TransportConnectionState.Connected -> AppConnectionAvailability.AVAILABLE
                    is TransportConnectionState.Failed -> AppConnectionAvailability.UNAVAILABLE
                    else -> null
                }
            }.distinctUntilChanged()
}
