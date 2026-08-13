package com.cbgm.securechat.feature.transport.connection

import kotlinx.coroutines.flow.StateFlow

interface TransportConnectionManager {
    val connectionState: StateFlow<TransportConnectionState>

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
}
