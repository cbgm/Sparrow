package com.cbgm.sparrow.feature.chats.data.direct.incoming.handler

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.MessageDeletionPacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource

class DirectMessageDeletionPacketHandler(
    private val chatDao: ChatDao,
    private val attachmentTransfer: MessageAttachmentDataSource
) {
    suspend fun handle(
        context: IncomingPacketContext,
        packet: MessageDeletionPacket
    ): Result<Unit> =
        runCatching {
            check(context.transportMode == TransportEncryptionMode.SEALED_BOX.name) {
                "Direct message deletion requires an encrypted Sparrow transport"
            }
            val target = chatDao.findMessageById(packet.messageId) ?: return@runCatching
            check(target.conversationId == context.conversationId) {
                "Deleted message belongs to another conversation"
            }
            check(!target.isMine && target.senderContactId == context.contactId) {
                "Only the original sender can delete a direct message"
            }
            attachmentTransfer.deleteForMessages(listOf(packet.messageId))
            chatDao.deleteMessagesAndRefreshConversations(listOf(target))
        }
}
