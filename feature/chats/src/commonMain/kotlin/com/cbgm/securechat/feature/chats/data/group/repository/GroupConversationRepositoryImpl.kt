package com.cbgm.securechat.feature.chats.data.group.repository

import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.MessageRecipientStateDao
import com.cbgm.securechat.feature.chats.data.group.mapper.toGroupConversation
import com.cbgm.securechat.feature.chats.domain.model.group.GroupConversation
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GroupConversationRepositoryImpl(
    private val chatDao: ChatDao,
    private val messageRecipientStateDao: MessageRecipientStateDao,
    private val groupInvitationDao: GroupInvitationDao
) : GroupConversationRepository {
    override fun observe(groupId: String): Flow<GroupConversation?> =
        combine(
            chatDao.observeConversationWithMessagesById(groupId),
            chatDao.observeConversationParticipants(groupId),
            messageRecipientStateDao.observeByConversationId(groupId),
            groupInvitationDao.observeByGroupId(groupId)
        ) { result, participants, recipientStates, invitations ->
            result
                ?.takeIf { it.conversation.type == GROUP_CONVERSATION_TYPE }
                ?.toGroupConversation(
                    participantContactIds = participants.map { it.contactId },
                    recipientStates = recipientStates,
                    invitations = invitations
                )
        }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
