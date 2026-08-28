package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.dao.MessageRecipientStateDao
import com.cbgm.sparrow.data.database.entity.ConversationEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.data.database.model.ConversationWithMessagesDto
import com.cbgm.sparrow.feature.attachments.data.datasource.MessageAttachmentDataSource
import com.cbgm.sparrow.feature.chats.data.group.mapper.toGroupConversation
import com.cbgm.sparrow.feature.chats.data.mapper.toMessagePartDtos
import com.cbgm.sparrow.feature.chats.data.model.MessagePartDto
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GroupConversationRepositoryImpl(
    private val chatDao: ChatDao,
    private val messageAttachmentDataSource: MessageAttachmentDataSource,
    private val messageRecipientStateDao: MessageRecipientStateDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val groupVerificationDao: GroupVerificationDao
) : GroupConversationRepository {
    override fun observe(groupId: String): Flow<GroupConversation?> {
        val messageSnapshot =
            combine(
                chatDao.observeConversationById(groupId),
                chatDao.observeRecentMessages(groupId, RECENT_MESSAGE_LIMIT),
                messageAttachmentDataSource.observeRecentByConversation(groupId, RECENT_MESSAGE_LIMIT)
            ) { conversation, recentMessages, attachmentsByMessageId ->
                MessageSnapshotDto(
                    conversation = conversation,
                    messages = recentMessages,
                    partsByMessageId =
                        attachmentsByMessageId.mapValues { (_, attachments) ->
                            attachments.toMessagePartDtos()
                        }
                )
            }

        return combine(
            messageSnapshot,
            groupSecurityDao.observeCurrentMemberKeys(groupId),
            messageRecipientStateDao.observeByConversationId(groupId),
            groupInvitationDao.observeByGroupId(groupId),
            groupVerificationDao.observeByGroupId(groupId)
        ) { snapshot, memberKeys, recipientStates, invitations, verificationRows ->
            snapshot.conversation
                ?.takeIf { it.type == GROUP_CONVERSATION_TYPE }
                ?.let { ConversationWithMessagesDto(it, snapshot.messages) }
                ?.toGroupConversation(
                    participantContactIds = memberKeys.map { memberKey -> memberKey.contactId },
                    recipientStates = recipientStates,
                    invitations = invitations,
                    verificationRows = verificationRows,
                    partsByMessageId = snapshot.partsByMessageId
                )
        }
    }

    private data class MessageSnapshotDto(
        val conversation: ConversationEntity?,
        val messages: List<MessageEntity>,
        val partsByMessageId: Map<String, List<MessagePartDto>>
    )

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val RECENT_MESSAGE_LIMIT = 500
    }
}
