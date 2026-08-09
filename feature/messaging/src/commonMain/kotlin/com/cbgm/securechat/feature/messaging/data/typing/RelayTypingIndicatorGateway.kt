package com.cbgm.securechat.feature.messaging.data.typing

import com.cbgm.securechat.feature.chats.domain.repository.TypingIndicatorGateway
import com.cbgm.securechat.feature.messaging.domain.relay.ContactRelayIdResolver
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class RelayTypingIndicatorGateway(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val contactRelayIdResolver: ContactRelayIdResolver
) : TypingIndicatorGateway {
    override fun observeTyping(contactId: String): Flow<Boolean> =
        webSocketTransportClient.incomingTypingEvents
            .transform { event ->
                val contactRelayId =
                    contactRelayIdResolver
                        .resolve(contactId = contactId)
                        .getOrNull()
                        ?: return@transform

                if (event.senderId == contactRelayId) {
                    emit(event.isTyping)
                }
            }

    override suspend fun sendTypingState(
        contactId: String,
        isTyping: Boolean
    ): Result<Unit> =
        contactRelayIdResolver
            .resolve(contactId = contactId)
            .fold(
                onSuccess = { contactRelayId ->
                    webSocketTransportClient.sendTypingState(
                        recipientId = contactRelayId,
                        isTyping = isTyping
                    )
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
}
