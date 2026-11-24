package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.domain.gateway.TransportPreferences
import com.cbgm.sparrow.feature.transport.domain.model.ConnectionMode
import com.cbgm.sparrow.feature.transport.domain.rule.keepsConnectionWhileClosed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How messages reach the device while Sparrow is closed; where that is impossible (iOS), only while it is open. */
class ObserveConnectionModeUseCase(
    private val preferences: TransportPreferences
) {
    operator fun invoke(): Flow<ConnectionMode> =
        preferences.observeConnectionMode().map { mode -> mode.takeIf { keepsConnectionWhileClosed() } ?: ConnectionMode.WHILE_OPEN }
}
