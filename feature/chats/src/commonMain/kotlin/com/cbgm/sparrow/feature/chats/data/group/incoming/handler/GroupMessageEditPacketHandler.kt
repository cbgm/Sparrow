package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.message.MessageEditPayloadCodec
import com.cbgm.sparrow.core.protocol.packet.GroupMessageEditPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.chats.data.group.security.GROUP_END_TO_END_ENCRYPTED_MODE
import com.cbgm.sparrow.feature.chats.data.group.security.GroupSecurityManager

class GroupMessageEditPacketHandler(
    private val chatDao: ChatDao,
    private val groupSecurityManager: GroupSecurityManager,
    private val messageEditPayloadCodec: MessageEditPayloadCodec,
    private val attachmentTransfer: MessageAttachmentDataSource
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupMessageEditPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val editPacket =
                packet as? GroupMessageEditPacket
                    ?: error("GroupMessageEditPacketHandler received an incompatible packet")
            val conversation =
                chatDao.findConversationById(editPacket.groupId)
                    ?: error("Group conversation was not found")
            check(conversation.type == GROUP_CONVERSATION_TYPE) { "Conversation is not a group" }

            val plaintext =
                groupSecurityManager
                    .decryptMessageEdit(editPacket, context.contactId)
                    .getOrThrow()
            val edit = messageEditPayloadCodec.decode(plaintext)
            val target = chatDao.findMessageById(edit.messageId) ?: return@runCatching
            check(target.conversationId == editPacket.groupId) {
                "Edited message belongs to another group"
            }
            check(target.transportMode == GROUP_END_TO_END_ENCRYPTED_MODE) {
                "Only user messages can be edited"
            }
            check(!target.isMine && target.senderContactId == context.contactId) {
                "Only the original sender can edit a group message"
            }
            check(target.text.isNotBlank()) { "Only text messages can be edited" }
            check(attachmentTransfer.protocolAttachments(edit.messageId).isEmpty()) {
                "Messages with attachments cannot be edited"
            }
            chatDao.upsertMessage(target.copy(text = edit.text.trim()))
        }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
