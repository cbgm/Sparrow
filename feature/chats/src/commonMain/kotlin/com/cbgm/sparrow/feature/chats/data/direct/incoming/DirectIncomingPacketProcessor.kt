package com.cbgm.sparrow.feature.chats.data.direct.incoming

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.MessageDeletionPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.direct.datasource.DirectConversationDataSource
import com.cbgm.sparrow.feature.chats.data.direct.incoming.handler.DirectMessageDeletionPacketHandler
import com.cbgm.sparrow.feature.chats.data.direct.incoming.handler.DirectMessagePacketHandler
import com.cbgm.sparrow.feature.chats.data.model.DecodedIncomingPacketDto
import com.cbgm.sparrow.feature.contacts.domain.model.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase

class DirectIncomingPacketProcessor(
    private val conversationDataSource: DirectConversationDataSource,
    private val messagePacketHandler: DirectMessagePacketHandler,
    private val deletionPacketHandler: DirectMessageDeletionPacketHandler,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase
) {
    fun canProcess(packet: SparrowPacket): Boolean =
        packet is ChatMessagePacket || packet is MessageDeletionPacket

    suspend fun process(incoming: DecodedIncomingPacketDto): Result<Unit> {
        val authorization = requireDirectChatAuthorization(incoming.contactId)
        authorization.exceptionOrNull()?.let { error ->
            return if (error is DirectChatAuthorizationRequiredException) {
                Result.success(Unit)
            } else {
                Result.failure(error)
            }
        }

        val conversationId = conversationDataSource.getOrCreate(incoming.contactId).id
        val context = incoming.toIncomingPacketContext(conversationId)
        return when (val packet = incoming.packet) {
            is ChatMessagePacket -> messagePacketHandler.handle(context, packet)
            is MessageDeletionPacket -> deletionPacketHandler.handle(context, packet)
            else -> error("DirectIncomingPacketProcessor received a non-direct packet")
        }
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
