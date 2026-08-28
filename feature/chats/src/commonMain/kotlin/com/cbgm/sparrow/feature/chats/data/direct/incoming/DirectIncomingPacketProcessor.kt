package com.cbgm.sparrow.feature.chats.data.direct.incoming

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.direct.datasource.DirectConversationDataSource
import com.cbgm.sparrow.feature.chats.data.direct.incoming.handler.DirectMessagePacketHandler
import com.cbgm.sparrow.feature.chats.data.model.DecodedIncomingPacketDto
import com.cbgm.sparrow.feature.contacts.domain.model.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase

class DirectIncomingPacketProcessor(
    private val conversationDataSource: DirectConversationDataSource,
    private val messagePacketHandler: DirectMessagePacketHandler,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase
) {
    fun canProcess(packet: SparrowPacket): Boolean =
        packet is ChatMessagePacket

    suspend fun process(incoming: DecodedIncomingPacketDto): Result<Unit> {
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

        val conversationId = conversationDataSource.getOrCreate(incoming.contactId).id
        return messagePacketHandler.handle(
            context = incoming.toIncomingPacketContext(conversationId),
            packet = packet
        )
    }

    private fun DecodedIncomingPacketDto.toIncomingPacketContext(conversationId: String): IncomingPacketContext =
        IncomingPacketContext(
            contactId = contactId,
            conversationId = conversationId,
            encodedTransportPayload = encodedTransportPayload,
            transportMode = transportMode,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )
}
