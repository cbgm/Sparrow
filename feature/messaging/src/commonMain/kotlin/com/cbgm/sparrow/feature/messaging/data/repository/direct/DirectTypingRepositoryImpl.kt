package com.cbgm.sparrow.feature.messaging.data.repository.direct

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectTypingRepository
import com.cbgm.sparrow.feature.messaging.application.routing.ContactRoutingIdResolver
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class DirectTypingRepositoryImpl(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val contactRoutingIdResolver: ContactRoutingIdResolver
) : DirectTypingRepository {
    override fun observe(contactId: String): Flow<Boolean> =
        webSocketTransportClient.incomingTypingEvents
            .transform { event ->
                val routingId = contactRoutingIdResolver.resolve(contactId).getOrNull() ?: return@transform
                if (event.senderId == routingId) {
                    emit(event.isTyping)
                }
            }

    override suspend fun send(
        contactId: String,
        isTyping: Boolean
    ): Result<Unit> =
        contactRoutingIdResolver
            .resolve(contactId)
            .fold(
                onSuccess = { routingId ->
                    webSocketTransportClient.sendTypingState(
                        recipientId = routingId,
                        isTyping = isTyping
                    )
                },
                onFailure = Result.Companion::failure
            )
}
