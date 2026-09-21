package com.cbgm.sparrow.feature.messaging.domain.usecase

import com.cbgm.sparrow.core.protocol.transport.OutgoingWireAcceptance
import com.cbgm.sparrow.core.protocol.transport.OutgoingWireSender

/** A generic transport operation. Routing, packet policy and key selection belong to orchestration. */
class SendEncodedTransportUseCase(
    private val outgoingWireSender: OutgoingWireSender
) {
    suspend operator fun invoke(
        recipientRoutingId: String,
        encodedTransportPayload: String
    ): Result<OutgoingWireAcceptance> =
        runCatching {
            require(recipientRoutingId.isNotBlank()) { "Recipient routing ID is required" }
            require(encodedTransportPayload.isNotBlank()) { "Encoded transport payload is required" }
            outgoingWireSender
                .sendWithAcceptance(recipientRoutingId, encodedTransportPayload)
                .getOrThrow()
        }
}
