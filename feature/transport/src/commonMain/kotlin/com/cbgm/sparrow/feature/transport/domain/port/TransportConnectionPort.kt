package com.cbgm.sparrow.feature.transport.domain.port

import com.cbgm.sparrow.feature.transport.domain.model.TransportConnectionState
import kotlinx.coroutines.flow.StateFlow

interface TransportConnectionPort {
    val connectionState: StateFlow<TransportConnectionState>

    /** The gateway node the app is connected through right now, or null while it is not connected. */
    val connectedNodeId: String?

    /**
     * Starts the persistent connection loop.
     *
     * The loop reconnects automatically after unexpected disconnects.
     */
    fun start()

    /**
     * Permanently stops the current connection loop.
     */
    suspend fun stop()

    /**
     * Reconnects when the running connection uses another node than [nodeId], so the new order of
     * nodes applies right away.
     */
    suspend fun moveTo(nodeId: String)
}
