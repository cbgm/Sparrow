package com.cbgm.securechat.feature.messaging.data.relay

import com.cbgm.securechat.feature.messaging.application.relay.IncomingRelayEnvelope
import com.cbgm.securechat.feature.messaging.application.relay.IncomingRelayGateway
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WebSocketIncomingRelayGateway(
    private val webSocketTransportClient: WebSocketTransportClient
) : IncomingRelayGateway {
    override val incomingEnvelopes: Flow<IncomingRelayEnvelope> =
        webSocketTransportClient.incomingEnvelopes.map { envelope ->
            IncomingRelayEnvelope(
                envelopeId = envelope.envelopeId,
                senderRelayId = envelope.senderId,
                encodedTransportPayload = envelope.payload
            )
        }

    override suspend fun acknowledge(envelopeId: String): Result<Unit> = webSocketTransportClient.acknowledgeIncomingEnvelope(envelopeId = envelopeId)
}
