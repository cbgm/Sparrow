package com.cbgm.sparrow.feature.chats.data.direct.incoming.handler

import com.cbgm.sparrow.core.crypto.transport.TransportEncryptionMode
import com.cbgm.sparrow.data.database.entity.MessageReactionEntity
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.data.datasource.MessageReactionDataSource
import com.cbgm.sparrow.feature.chats.data.direct.datasource.DirectConversationDataSource
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.packet.OperationMessagePacket

class DirectOperationMessagePacketHandler(
    private val conversationDataSource: DirectConversationDataSource,
    private val messageReactionDataSource: MessageReactionDataSource,
    private val attachmentTransfer: MessageAttachmentOperationsRepository
) {
    suspend fun handle(
        context: IncomingPacketContext,
        packet: OperationMessagePacket
    ): Result<Unit> =
        runCatching {
            check(context.transportMode == TransportEncryptionMode.SEALED_BOX.name) {
                "Direct message operations require an encrypted Sparrow transport"
            }

            when (val operation = packet.message.operation) {
                is MessageOperation.Edit -> {
                    val target = conversationDataSource.findMessageById(operation.messageId) ?: return@runCatching
                    check(target.conversationId == context.conversationId) {
                        "Edited message belongs to another conversation"
                    }
                    check(!target.isMine && target.senderContactId == context.contactId) {
                        "Only the original sender can edit a direct message"
                    }
                    check(!conversationDataSource.findMessageText(operation.messageId).isNullOrBlank()) {
                        "Only text messages can be edited"
                    }
                    check(attachmentTransfer.messageParts(operation.messageId).getOrThrow().isEmpty()) {
                        "Messages with attachments cannot be edited"
                    }
                    conversationDataSource.replaceMessageText(operation.messageId, operation.text.trim())
                }

                is MessageOperation.Delete -> {
                    val target = conversationDataSource.findMessageById(operation.messageId) ?: return@runCatching
                    check(target.conversationId == context.conversationId) {
                        "Deleted message belongs to another conversation"
                    }
                    check(!target.isMine && target.senderContactId == context.contactId) {
                        "Only the original sender can delete a direct message"
                    }
                    attachmentTransfer.deleteForMessages(listOf(operation.messageId))
                    conversationDataSource.deleteMessages(listOf(target))
                }

                is MessageOperation.Reaction -> {
                    val target = conversationDataSource.findMessageById(operation.messageId) ?: return@runCatching
                    check(target.conversationId == context.conversationId) {
                        "Reaction target belongs to another conversation"
                    }
                    if (operation.removed) {
                        messageReactionDataSource.delete(operation.messageId, context.contactId, operation.emoji)
                    } else {
                        messageReactionDataSource.upsert(
                            MessageReactionEntity(
                                operation.messageId,
                                context.conversationId,
                                context.contactId,
                                operation.emoji
                            )
                        )
                    }
                }

                is MessageOperation.PollVote,
                is MessageOperation.PollClose -> error("Poll operations are only supported in groups")
            }
        }
}
