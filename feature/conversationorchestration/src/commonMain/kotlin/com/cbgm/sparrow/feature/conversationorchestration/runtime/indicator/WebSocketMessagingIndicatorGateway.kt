package com.cbgm.sparrow.feature.conversationorchestration.runtime.indicator

import com.cbgm.sparrow.feature.messaging.domain.model.MessagingIndicator
import com.cbgm.sparrow.feature.messaging.runtime.indicator.MessagingIndicatorGateway
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

    override suspend fun send(recipientRoutingId: String, indicatorType: String): Result<Unit> =
        client.sendIndicatorState(recipientId = recipientRoutingId, indicatorType = indicatorType)
}
