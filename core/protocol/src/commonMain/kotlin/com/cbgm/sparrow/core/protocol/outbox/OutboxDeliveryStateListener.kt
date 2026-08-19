package com.cbgm.sparrow.core.protocol.outbox

/**
 * Receives outbox lifecycle changes.
 *
 * The protocol layer does not know anything about Room chat messages.
 * Feature implementations may use packetId to update their own state.
 */
interface OutboxDeliveryStateListener {
    suspend fun onProcessing(packetId: String): Result<Unit>

    suspend fun onPrepared(
        packetId: String,
        encodedTransportPayload: String,
        transportMode: String
    ): Result<Unit>

    suspend fun onSent(packetId: String): Result<Unit>

    suspend fun onExpired(packetId: String): Result<Unit> = Result.success(Unit)

    suspend fun onFailed(
        packetId: String,
        errorMessage: String
    ): Result<Unit>
}
