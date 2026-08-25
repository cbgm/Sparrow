package com.cbgm.sparrow.feature.chats.data.direct.incoming

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.direct.incoming.handler.DirectMessagePacketHandler
import com.cbgm.sparrow.feature.chats.data.direct.storage.DirectConversationStorage
import com.cbgm.sparrow.feature.chats.data.model.DecodedIncomingPacket
import com.cbgm.sparrow.feature.contacts.domain.model.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase

class DirectIncomingPacketProcessor(
    private val conversationStorage: DirectConversationStorage,
    private val messagePacketHandler: DirectMessagePacketHandler,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase
) {
    fun canProcess(packet: SparrowPacket): Boolean =
        packet is ChatMessagePacket

    suspend fun process(incoming: DecodedIncomingPacket): Result<Unit> {
        val packet =
            incoming.packet as? ChatMessagePacket
                ?: error("DirectIncomingPacketProcessor received a non-direct packet")
        val authorization = requireDirectChatAuthorization(incoming.contactId)
        authorization.exceptionOrNull()?.let { error ->
            return if (error is DirectChatAuthorizationRequiredException) {
                Result.success(Unit)
            } else {
                Result.failure(error)
            }
        }

        val conversationId = conversationStorage.getOrCreate(incoming.contactId).id
        return messagePacketHandler.handle(
            context = incoming.toContext(conversationId),
            packet = packet
        )
    }

    private fun DecodedIncomingPacket.toContext(conversationId: String): IncomingPacketContext =
        IncomingPacketContext(
            contactId = contactId,
            conversationId = conversationId,
            encodedTransportPayload = encodedTransportPayload,
            transportMode = transportMode,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )
}
