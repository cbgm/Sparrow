package com.cbgm.sparrow.startup.domain.usecase

import com.cbgm.sparrow.feature.transport.connection.TransportConnectionManager
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.startup.domain.model.AppConnectionAvailability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveAppConnectionAvailabilityUseCase(
    private val transportConnectionManager: TransportConnectionManager
) {
    operator fun invoke(): Flow<AppConnectionAvailability> =
        transportConnectionManager
            .connectionState
            .map { state ->
                if (state is TransportConnectionState.Connected) {
                    AppConnectionAvailability.AVAILABLE
                } else {
                    AppConnectionAvailability.UNAVAILABLE
                }
            }.distinctUntilChanged()
}
