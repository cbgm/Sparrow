package com.cbgm.sparrow.feature.messaging.data.routing

import com.cbgm.sparrow.feature.messaging.application.routing.IncomingEnvelopeGateway
import com.cbgm.sparrow.feature.messaging.application.routing.IncomingTransportEnvelope
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WebSocketIncomingEnvelopeGateway(
    private val webSocketTransportClient: WebSocketTransportClient
) : IncomingEnvelopeGateway {
    override val incomingEnvelopes: Flow<IncomingTransportEnvelope> =
        webSocketTransportClient.incomingEnvelopes.map { envelope ->
            IncomingTransportEnvelope(
                envelopeId = envelope.envelopeId,
                senderRoutingId = envelope.senderId,
                encodedTransportPayload = envelope.payload
            )
        }

    override suspend fun acknowledge(envelopeId: String): Result<Unit> = webSocketTransportClient.acknowledgeIncomingEnvelope(envelopeId = envelopeId)
}
