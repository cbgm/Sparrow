package com.cbgm.securechat.feature.chats.data.outbox

import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.feature.chats.data.direct.delivery.DirectOutboxDeliveryHandler
import com.cbgm.securechat.feature.chats.data.group.delivery.GroupOutboxDeliveryHandler
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent

/** Thin outbox edge that routes chat callbacks to Direct or Group. */
class ChatOutboxDeliveryStateRouter(
    private val directHandler: DirectOutboxDeliveryHandler,
    private val groupHandler: GroupOutboxDeliveryHandler
) : OutboxDeliveryStateListener {
    override suspend fun onProcessing(packetId: String): Result<Unit> =
        applyEvent(packetId, MessageDeliveryEvent.SEND_STARTED)

    override suspend fun onPrepared(
        packetId: String,
        encodedTransportPayload: String,
        transportMode: String
    ): Result<Unit> =
        runCatching {
            if (directHandler.canHandle(packetId)) {
                directHandler.onPrepared(
                    packetId = packetId,
                    encodedTransportPayload = encodedTransportPayload,
                    transportMode = transportMode
                )
            }
        }

    override suspend fun onSent(packetId: String): Result<Unit> =
        applyEvent(packetId, MessageDeliveryEvent.SEND_SUCCEEDED)

    override suspend fun onFailed(
        packetId: String,
        errorMessage: String
    ): Result<Unit> =
        applyEvent(
            packetId = packetId,
            event = MessageDeliveryEvent.SEND_FAILED,
            errorMessage = errorMessage
        )

    private suspend fun applyEvent(
        packetId: String,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ): Result<Unit> =
        runCatching {
            when {
                groupHandler.canHandle(packetId) ->
                    groupHandler.applyEvent(packetId, event, errorMessage)

                directHandler.canHandle(packetId) ->
                    directHandler.applyEvent(packetId, event, errorMessage)
            }
        }
}
