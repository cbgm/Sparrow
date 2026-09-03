package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.message.MessageDeletionPayloadCodec
import com.cbgm.sparrow.core.protocol.packet.GroupMessageDeletionPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager

class GroupMessageDeletionPacketHandler(
    private val chatDao: ChatDao,
    private val groupSecurityManager: GroupSecurityManager,
    private val messageDeletionPayloadCodec: MessageDeletionPayloadCodec,
    private val attachmentTransfer: MessageAttachmentDataSource
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
                chatDao.findConversationById(deletionPacket.groupId)
                    ?: error("Group conversation was not found")
            check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }

            val plaintext =
                groupSecurityManager
                    .decryptMessageDeletion(
                        packet = deletionPacket,
                        senderContactId = context.contactId
                    ).getOrThrow()
            val deletion = messageDeletionPayloadCodec.decode(plaintext)
            val target = chatDao.findMessageById(deletion.messageId) ?: return@runCatching
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
            chatDao.deleteMessagesAndRefreshConversations(listOf(target))
        }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
