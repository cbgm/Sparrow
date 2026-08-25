package com.cbgm.sparrow.feature.messaging.data.repository

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupTypingRepository
import com.cbgm.sparrow.feature.messaging.data.datasource.GroupRoutingDataSource
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class GroupTypingRepositoryImpl(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val groupRoutingDataSource: GroupRoutingDataSource
) : GroupTypingRepository {
    override fun observeMember(
        groupId: String,
        contactId: String
    ): Flow<Boolean> =
        webSocketTransportClient.incomingTypingEvents
            .transform { event ->
                val routingId =
                    groupRoutingDataSource
                        .resolve(groupId, contactId)
                        .getOrNull()
                        ?: return@transform
                if (event.senderId == routingId) {
                    emit(event.isTyping)
                }
            }

    override suspend fun setTyping(
        groupId: String,
        isTyping: Boolean
    ): Result<Unit> =
        runCatching {
            val recipientRoutingIds =
                groupRoutingDataSource
                    .resolveMembers(groupId)
                    .getOrThrow()
                    .values

            var firstFailure: Throwable? = null
            recipientRoutingIds.forEach { routingId ->
                webSocketTransportClient
                    .sendTypingState(
                        recipientId = routingId,
                        isTyping = isTyping
                    ).exceptionOrNull()
                    ?.let { error ->
                        if (firstFailure == null) firstFailure = error
                    }
            }
            firstFailure?.let { error -> throw error }
        }
}
