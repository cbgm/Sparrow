package com.cbgm.sparrow.feature.conversationorchestration.runtime.indicator

import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import com.cbgm.sparrow.feature.messaging.runtime.indicator.MessagingIndicatorGateway
import com.cbgm.sparrow.feature.transport.connection.TransportConnectionState
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Adapts the existing transport stream, without exposing transport types to Messaging. */
class WebSocketMessagingIndicatorGateway(
    private val client: WebSocketTransportClient
) : MessagingIndicatorGateway {
    override val incoming: Flow<MessagingIndicator> =
        client.incomingIndicatorEvents.map { event ->
            MessagingIndicator(senderRoutingId = event.senderId, indicatorType = event.indicatorType)
        }

    override suspend fun send(recipientRoutingId: String, indicatorType: String): Result<Unit> {
        // Typing/recording indicators are ephemeral, not durable messages.
        // A disconnected transport is expected while the public node reconnects.
        if (client.connectionState.value !is TransportConnectionState.Connected) {
            return Result.success(Unit)
        }
        val sent = client.sendIndicatorState(recipientId = recipientRoutingId, indicatorType = indicatorType)
        // Connection can disappear between the pre-check and the actual send.
        // Ignore only this precise transient condition, never unexpected failures.
        val error = sent.exceptionOrNull()
        if (error is IllegalStateException &&
            error.message == "WebSocket transport is not connected" &&
            client.connectionState.value !is TransportConnectionState.Connected
        ) {
            return Result.success(Unit)
        }
        return sent
    }
}
