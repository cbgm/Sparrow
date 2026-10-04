package com.cbgm.sparrow.protocol.packet

import kotlinx.serialization.Serializable

@Serializable
sealed interface SparrowPacket {
    /**
     * Unique identifier for deduplication, acknowledgements,
     * tracing and replay protection.
     */
    val packetId: String

    /**
     * Protocol schema version.
     */
    val version: Int
}
