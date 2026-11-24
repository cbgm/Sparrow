package com.cbgm.sparrow.feature.transport.domain.port

import com.cbgm.sparrow.feature.transport.domain.model.CircuitStatus
import kotlinx.coroutines.flow.StateFlow

/** The app's onion circuits: they carry its mailbox and file requests, so no node sees both its address and where they go. */
interface CircuitPort {
    val status: StateFlow<CircuitStatus>

    /** Whether a circuit through at least [minimumHopCount] hops is open right now. */
    fun hasCircuit(minimumHopCount: Int = 1): Boolean

    /** Builds the circuits and keeps them open until cancelled. */
    suspend fun run()
}
