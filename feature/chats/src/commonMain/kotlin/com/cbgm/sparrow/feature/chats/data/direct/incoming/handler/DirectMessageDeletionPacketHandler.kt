package com.cbgm.sparrow.feature.chats.data.direct.incoming.handler

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.MessageDeletionPacket
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.data.datasource.IncomingMessageDataSource

class DirectMessageDeletionPacketHandler(
    private val incomingMessageDataSource: IncomingMessageDataSource,
    private val attachmentTransfer: MessageAttachmentOperationsRepository
) {
    suspend fun handle(
        context: IncomingPacketContext,
        packet: MessageDeletionPacket
    ): Result<Unit> =
        runCatching {
            check(context.transportMode == TransportEncryptionMode.SEALED_BOX.name) {
                "Direct message deletion requires an encrypted Sparrow transport"
            }
            val target = incomingMessageDataSource.findMessage(packet.messageId) ?: return@runCatching
            check(target.conversationId == context.conversationId) {
                "Deleted message belongs to another conversation"
            }
            check(!target.isMine && target.senderContactId == context.contactId) {
                "Only the original sender can delete a direct message"
            }
            attachmentTransfer.deleteForMessages(listOf(packet.messageId))
            incomingMessageDataSource.deleteMessages(listOf(target))
        }
}
