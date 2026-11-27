package com.cbgm.sparrow.feature.transport.domain.port

import com.cbgm.sparrow.feature.transport.domain.model.CircuitHopCount
import com.cbgm.sparrow.feature.transport.domain.model.ProtectedRouteMissingException
import kotlinx.coroutines.flow.Flow

/**
 * Whether something may leave the device without a circuit through at least [CircuitHopCount.PROTECTING] hops: only
 * when the user allows sending directly. A circuit through one hop does not protect: its single node sees both the
 * device's address and where the request goes.
 */
interface DirectSendingPort {
    /** True while nothing may go out unprotected and no protecting circuit is up, so messages wait. */
    fun observeWaiting(): Flow<Boolean>

    suspend fun sendsDirectly(): Boolean

    /** For a send that cannot use the circuit: throws [ProtectedRouteMissingException] while it may not go out. */
    suspend fun checkUnprotectedSendAllowed()

    /** Whether a circuit through at least [CircuitHopCount.PROTECTING] hops is up, so a send hides the device's address. */
    fun hasProtectingCircuit(): Boolean
}
