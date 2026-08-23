package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.dao.MessageAttachmentDao
import com.cbgm.sparrow.data.database.dao.MessageRecipientStateDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.MessageAttachmentEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.model.ConversationWithMessages
import com.cbgm.sparrow.feature.chats.data.attachment.toDomainAttachmentsByMessageId
import com.cbgm.sparrow.feature.chats.data.group.mapper.toGroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GroupConversationRepositoryImpl(
    private val chatDao: ChatDao,
    private val messageAttachmentDao: MessageAttachmentDao,
    private val messageRecipientStateDao: MessageRecipientStateDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao
) : GroupConversationRepository {
    override fun observe(groupId: String): Flow<GroupConversation?> {
        val messageSnapshot =
            combine(
                chatDao.observeConversationById(groupId),
                chatDao.observeRecentMessages(groupId, RECENT_MESSAGE_LIMIT),
                messageAttachmentDao.observeRecentByConversation(groupId, RECENT_MESSAGE_LIMIT)
            ) { conversation, recentMessages, attachments ->
                MessageSnapshot(
                    conversation = conversation,
                    messages = recentMessages,
                    attachments = attachments
                )
            }

        return combine(
            messageSnapshot,
            groupSecurityDao.observeCurrentMemberKeys(groupId),
            messageRecipientStateDao.observeByConversationId(groupId),
            groupInvitationDao.observeByGroupId(groupId)
        ) { snapshot, memberKeys, recipientStates, invitations ->
            snapshot.conversation
                ?.takeIf { it.type == GROUP_CONVERSATION_TYPE }
                ?.let { ConversationWithMessages(it, snapshot.messages) }
                ?.toGroupConversation(
                    participantContactIds = memberKeys.map { memberKey -> memberKey.contactId },
                    recipientStates = recipientStates,
                    invitations = invitations,
                    attachmentsByMessageId = snapshot.attachments.toDomainAttachmentsByMessageId()
                )
        }
    }

    private data class MessageSnapshot(
        val conversation: ConversationEntity?,
        val messages: List<MessageEntity>,
        val attachments: List<MessageAttachmentEntity>
    )

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val RECENT_MESSAGE_LIMIT = 500
    }
}
