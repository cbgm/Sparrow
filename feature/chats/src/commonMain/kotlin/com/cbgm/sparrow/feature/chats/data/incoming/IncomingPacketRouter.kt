package com.cbgm.sparrow.feature.chats.data.incoming

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.ProtocolPacketHandler
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.feature.chats.data.direct.incoming.DirectIncomingPacketProcessor
import com.cbgm.sparrow.feature.chats.data.group.incoming.GroupIncomingPacketProcessor
import com.cbgm.sparrow.feature.chats.data.model.DecodedIncomingPacketDto

/**
 * Routes a decoded protocol packet to exactly one feature path.
 *
 * Direct and Group never share packet handlers. The fallback is only for
 * non-chat packet families provided by other features.
 */
class IncomingPacketRouter(
    private val directProcessor: DirectIncomingPacketProcessor,
    private val groupProcessor: GroupIncomingPacketProcessor,
    private val receiptRouter: ReceiptIncomingPacketRouter,
    private val fallbackPacketHandler: ProtocolPacketHandler,
    private val chatDao: ChatDao
) {
    suspend fun route(incoming: DecodedIncomingPacketDto): Result<Unit> =
        when {
            directProcessor.canProcess(incoming.packet) -> directProcessor.process(incoming)
            groupProcessor.canProcess(incoming.packet) -> groupProcessor.process(incoming)
            receiptRouter.canRoute(incoming.packet) -> receiptRouter.route(incoming)
            else -> processFallback(incoming)
        }

    private suspend fun processFallback(incoming: DecodedIncomingPacketDto): Result<Unit> =
        fallbackPacketHandler.handle(
            context = incoming.toFallbackContext(),
            packet = incoming.packet
        )

    private suspend fun DecodedIncomingPacketDto.toFallbackContext(): IncomingPacketContext {
        val conversationId =
            chatDao.findConversationByContactId(contactId)?.id
                ?: "control-${packet.packetId}"
        return IncomingPacketContext(
            contactId = contactId,
            conversationId = conversationId,
            encodedTransportPayload = encodedTransportPayload,
            transportMode = transportMode,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )
    }
}
