package com.cbgm.sparrow.feature.chats.data.direct.incoming.handler

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.MessageEditPacket
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.data.datasource.IncomingMessageDataSource

class DirectMessageEditPacketHandler(
    private val incomingMessageDataSource: IncomingMessageDataSource,
    private val attachmentTransfer: MessageAttachmentOperationsRepository
) {
    suspend fun handle(
        context: IncomingPacketContext,
        packet: MessageEditPacket
    ): Result<Unit> =
        runCatching {
            check(context.transportMode == TransportEncryptionMode.SEALED_BOX.name) {
                "Direct message edit requires an encrypted Sparrow transport"
            }
            val target = incomingMessageDataSource.findMessage(packet.messageId) ?: return@runCatching
            check(target.conversationId == context.conversationId) {
                "Edited message belongs to another conversation"
            }
            check(!target.isMine && target.senderContactId == context.contactId) {
                "Only the original sender can edit a direct message"
            }
            check(target.text.isNotBlank()) { "Only text messages can be edited" }
            check(attachmentTransfer.protocolAttachments(packet.messageId).isEmpty()) {
                "Messages with attachments cannot be edited"
            }
            incomingMessageDataSource.saveMessage(target.copy(text = packet.text.trim()))
        }
}
