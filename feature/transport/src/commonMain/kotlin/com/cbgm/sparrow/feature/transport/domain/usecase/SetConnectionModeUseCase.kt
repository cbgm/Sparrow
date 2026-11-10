package com.cbgm.sparrow.feature.transport.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.transport.background.ConnectionMode
import com.cbgm.sparrow.feature.transport.background.ConnectionModeStore
import com.cbgm.sparrow.feature.transport.background.keepsConnectionWhileClosed

/** Chooses how messages reach the device while Sparrow is closed; on iOS only while it is open is possible. */
class SetConnectionModeUseCase(
    private val store: ConnectionModeStore
) {
    suspend operator fun invoke(mode: ConnectionMode): Result<Unit> =
        safeSuspendCall {
            require(mode == ConnectionMode.WHILE_OPEN || keepsConnectionWhileClosed()) { "This device cannot keep a connection while Sparrow is closed" }
            store.set(mode)
        }
}
