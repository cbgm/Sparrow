package com.cbgm.securechat.feature.chats.data.direct.incoming

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.packet.ChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.chats.data.direct.incoming.handler.DirectMessagePacketHandler
import com.cbgm.securechat.feature.chats.data.direct.storage.DirectConversationStorage
import com.cbgm.securechat.feature.chats.data.model.DecodedIncomingPacket
import com.cbgm.securechat.feature.contacts.domain.model.DirectChatAuthorizationRequiredException

class DirectIncomingPacketProcessor(
    private val conversationStorage: DirectConversationStorage,
    private val messagePacketHandler: DirectMessagePacketHandler
) {
    fun canProcess(packet: SecureChatPacket): Boolean =
        packet is ChatMessagePacket

    suspend fun process(incoming: DecodedIncomingPacket): Result<Unit> {
        val packet =
            incoming.packet as? ChatMessagePacket
                ?: error("DirectIncomingPacketProcessor received a non-direct packet")
        val conversationId = conversationStorage.getOrCreate(incoming.contactId).id
        val result =
            messagePacketHandler.handle(
                context = incoming.toContext(conversationId),
                packet = packet
            )
        return if (result.exceptionOrNull() is DirectChatAuthorizationRequiredException) {
            Result.success(Unit)
        } else {
            result
        }
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
