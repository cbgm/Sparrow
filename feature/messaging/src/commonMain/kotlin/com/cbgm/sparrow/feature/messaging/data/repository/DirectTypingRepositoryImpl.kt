package com.cbgm.sparrow.feature.messaging.data.repository

import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectTypingRepository
import com.cbgm.sparrow.feature.messaging.data.datasource.ContactRoutingDataSource
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class DirectTypingRepositoryImpl(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val contactRoutingDataSource: ContactRoutingDataSource
) : DirectTypingRepository {
    override fun observe(contactId: String): Flow<Boolean> =
        webSocketTransportClient.incomingTypingEvents
            .transform { event ->
                val routingId = contactRoutingDataSource.resolve(contactId).getOrNull() ?: return@transform
                if (event.senderId == routingId) {
                    emit(event.isTyping)
                }
            }

    override suspend fun send(
        contactId: String,
        isTyping: Boolean
    ): Result<Unit> =
        contactRoutingDataSource
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
