package com.cbgm.securechat.feature.messaging.data.repository.group

import com.cbgm.securechat.feature.chats.domain.repository.group.GroupTypingRepository
import com.cbgm.securechat.feature.messaging.domain.relay.ContactRelayIdResolver
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class GroupTypingRepositoryImpl(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val contactRelayIdResolver: ContactRelayIdResolver
) : GroupTypingRepository {
    override fun observeMember(contactId: String): Flow<Boolean> =
        webSocketTransportClient.incomingTypingEvents
            .transform { event ->
                val relayId = contactRelayIdResolver.resolve(contactId).getOrNull() ?: return@transform
                if (event.senderId == relayId) {
                    emit(event.isTyping)
                }
            }

    override suspend fun sendToMembers(
        contactIds: Set<String>,
        isTyping: Boolean
    ): Result<Unit> =
        runCatching {
            val failures =
                contactIds.mapNotNull { contactId ->
                    sendToMember(contactId, isTyping).exceptionOrNull()
                }
            failures.firstOrNull()?.let { error -> throw error }
        }

    private suspend fun sendToMember(
        contactId: String,
        isTyping: Boolean
    ): Result<Unit> =
        runCatching {
            val relayId = contactRelayIdResolver.resolve(contactId).getOrThrow()
            webSocketTransportClient
                .sendTypingState(
                    recipientId = relayId,
                    isTyping = isTyping
                ).getOrThrow()
        }
}
