package com.cbgm.securechat.feature.messaging.data.repository.group

import com.cbgm.securechat.feature.chats.domain.repository.group.GroupTypingRepository
import com.cbgm.securechat.feature.messaging.application.relay.GroupRelayIdResolver
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class GroupTypingRepositoryImpl(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val groupRelayIdResolver: GroupRelayIdResolver
) : GroupTypingRepository {
    override fun observeMember(
        groupId: String,
        contactId: String
    ): Flow<Boolean> =
        webSocketTransportClient.incomingTypingEvents
            .transform { event ->
                val relayId =
                    groupRelayIdResolver
                        .resolve(groupId, contactId)
                        .getOrNull()
                        ?: return@transform
                if (event.senderId == relayId) {
                    emit(event.isTyping)
                }
            }

    override suspend fun setTyping(
        groupId: String,
        isTyping: Boolean
    ): Result<Unit> =
        runCatching {
            val recipientRelayIds =
                groupRelayIdResolver
                    .resolveMembers(groupId)
                    .getOrThrow()
                    .values

            var firstFailure: Throwable? = null
            recipientRelayIds.forEach { relayId ->
                webSocketTransportClient
                    .sendTypingState(
                        recipientId = relayId,
                        isTyping = isTyping
                    ).exceptionOrNull()
                    ?.let { error ->
                        if (firstFailure == null) firstFailure = error
                    }
            }
            firstFailure?.let { error -> throw error }
        }
}
