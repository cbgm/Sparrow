package com.cbgm.sparrow.feature.transport.sender

import com.cbgm.sparrow.core.protocol.transport.OutgoingWireSender

/**
 * Temporary sender used until a real network transport is connected.
 *
 * This deliberately returns failure so queued packets are not marked
 * SENT when they were never delivered.
 */
class UnavailableOutgoingWireSender : OutgoingWireSender {
    override suspend fun send(
        recipientAddress: String,
        encodedTransportPayload: String
    ): Result<Unit> =
        Result.failure(
            IllegalStateException(
                "No outgoing Sparrow transport is configured"
            )
        )
}
