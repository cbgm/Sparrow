package com.cbgm.securechat.feature.messaging.data.repository.group

import com.cbgm.securechat.feature.chats.domain.repository.group.GroupTypingRepository
import com.cbgm.securechat.feature.messaging.application.routing.GroupRoutingIdResolver
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class GroupTypingRepositoryImpl(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val groupRoutingIdResolver: GroupRoutingIdResolver
) : GroupTypingRepository {
    override fun observeMember(
        groupId: String,
        contactId: String
    ): Flow<Boolean> =
        webSocketTransportClient.incomingTypingEvents
            .transform { event ->
                val routingId =
                    groupRoutingIdResolver
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
                groupRoutingIdResolver
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
