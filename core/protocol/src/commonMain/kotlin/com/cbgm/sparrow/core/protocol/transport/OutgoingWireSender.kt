package com.cbgm.sparrow.core.protocol.transport

/**
 * Server acknowledgement for one accepted wire envelope.
 *
 * [expiresAtEpochMilliseconds] is the server-declared deadline until which the
 * accepted envelope remains eligible for delivery. Until that deadline the
 * server owns delivery retries; the client must not invent an earlier failure.
 */
data class OutgoingWireAcceptance(
    val expiresAtEpochMilliseconds: Long
) {
    init {
        require(expiresAtEpochMilliseconds > 0L) {
            "Wire acceptance expiry must be positive"
        }
    }
}

/** Sends one completely encoded Sparrow transport payload. */
interface OutgoingWireSender {
    suspend fun send(
        recipientAddress: String,
        encodedTransportPayload: String
    ): Result<Unit>

    /**
     * Sends and returns the server-owned delivery deadline when the transport
     * can expose it. Non-server transports may use the default implementation.
     */
    suspend fun sendWithAcceptance(
        recipientAddress: String,
        encodedTransportPayload: String
    ): Result<OutgoingWireAcceptance> =
        send(recipientAddress, encodedTransportPayload).map {
            OutgoingWireAcceptance(Long.MAX_VALUE)
        }
}
