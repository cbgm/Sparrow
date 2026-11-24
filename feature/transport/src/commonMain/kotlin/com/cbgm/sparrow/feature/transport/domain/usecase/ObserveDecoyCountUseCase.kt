package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.TransportPreferences
import kotlinx.coroutines.flow.Flow

/** How many decoy envelopes the user chose to send with each send (0: off). */
class ObserveDecoyCountUseCase(
    private val preferences: TransportPreferences
) {
    operator fun invoke(): Flow<Int> = preferences.observeDecoyCount()
}
