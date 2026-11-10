package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.feature.transport.background.ConnectionMode
import com.cbgm.sparrow.feature.transport.background.ConnectionModeStore
import com.cbgm.sparrow.feature.transport.background.keepsConnectionWhileClosed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How messages reach the device while Sparrow is closed; where that is impossible (iOS), only while it is open. */
class ObserveConnectionModeUseCase(
    private val store: ConnectionModeStore
) {
    operator fun invoke(): Flow<ConnectionMode> =
        store.observe().map { mode -> mode.takeIf { keepsConnectionWhileClosed() } ?: ConnectionMode.WHILE_OPEN }
}
