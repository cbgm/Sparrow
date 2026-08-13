package com.cbgm.securechat.feature.messaging.data.repository.direct

import com.cbgm.securechat.feature.chats.domain.repository.direct.DirectTypingRepository
import com.cbgm.securechat.feature.messaging.application.relay.ContactRelayIdResolver
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class DirectTypingRepositoryImpl(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val contactRelayIdResolver: ContactRelayIdResolver
) : DirectTypingRepository {
    override fun observe(contactId: String): Flow<Boolean> =
        webSocketTransportClient.incomingTypingEvents
            .transform { event ->
                val relayId = contactRelayIdResolver.resolve(contactId).getOrNull() ?: return@transform
                if (event.senderId == relayId) {
                    emit(event.isTyping)
                }
            }

    override suspend fun send(
        contactId: String,
        isTyping: Boolean
    ): Result<Unit> =
        contactRelayIdResolver
            .resolve(contactId)
            .fold(
                onSuccess = { relayId ->
                    webSocketTransportClient.sendTypingState(
                        recipientId = relayId,
                        isTyping = isTyping
                    )
                },
                onFailure = Result.Companion::failure
            )
}
