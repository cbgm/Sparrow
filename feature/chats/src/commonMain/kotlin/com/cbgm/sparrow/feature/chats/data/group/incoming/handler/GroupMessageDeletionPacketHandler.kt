package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.message.MessageDeletionPayloadCodec
import com.cbgm.sparrow.core.protocol.packet.GroupMessageDeletionPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.chats.data.datasource.IncomingMessageDataSource
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.sparrow.feature.membership.domain.repository.GroupSecurityRepository

class GroupMessageDeletionPacketHandler(
    private val incomingMessageDataSource: IncomingMessageDataSource,
    private val groupSecurityManager: GroupSecurityRepository,
    private val messageDeletionPayloadCodec: MessageDeletionPayloadCodec,
    private val attachmentTransfer: MessageAttachmentOperationsRepository
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupMessageDeletionPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val deletionPacket =
                packet as? GroupMessageDeletionPacket
                    ?: error("GroupMessageDeletionPacketHandler received an incompatible packet")
            val conversation =
                incomingMessageDataSource.findConversation(deletionPacket.groupId)
                    ?: error("Group conversation was not found")
            check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }

            val plaintext =
                groupSecurityManager
                    .decryptMessageDeletion(
                        packet = deletionPacket,
                        senderContactId = context.contactId
                    ).getOrThrow()
            val deletion = messageDeletionPayloadCodec.decode(plaintext)
            val target = incomingMessageDataSource.findMessage(deletion.messageId) ?: return@runCatching
            check(target.conversationId == deletionPacket.groupId) {
                "Deleted message belongs to another group"
            }
            check(target.transportMode == GROUP_END_TO_END_ENCRYPTED_MODE) {
                "Only user messages can be deleted"
            }
            check(!target.isMine && target.senderContactId == context.contactId) {
                "Only the original sender can delete a group message"
            }
            attachmentTransfer.deleteForMessages(listOf(deletion.messageId))
            incomingMessageDataSource.deleteMessages(listOf(target))
        }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
