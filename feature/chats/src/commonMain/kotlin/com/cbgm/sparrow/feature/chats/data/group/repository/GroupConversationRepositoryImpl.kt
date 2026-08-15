package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupInvitationDao
import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.dao.MessageRecipientStateDao
import com.cbgm.sparrow.data.database.model.ConversationWithMessages
import com.cbgm.sparrow.feature.chats.data.group.mapper.toGroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GroupConversationRepositoryImpl(
    private val chatDao: ChatDao,
    private val messageRecipientStateDao: MessageRecipientStateDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao
) : GroupConversationRepository {
    override fun observe(groupId: String): Flow<GroupConversation?> =
        combine(
            chatDao.observeConversationById(groupId),
            chatDao.observeRecentMessages(groupId, RECENT_MESSAGE_LIMIT),
            groupSecurityDao.observeCurrentMemberKeys(groupId),
            messageRecipientStateDao.observeByConversationId(groupId),
            groupInvitationDao.observeByGroupId(groupId)
        ) { conversation, recentMessages, memberKeys, recipientStates, invitations ->
            conversation
                ?.takeIf { it.type == GROUP_CONVERSATION_TYPE }
                ?.let { ConversationWithMessages(it, recentMessages) }
                ?.toGroupConversation(
                    participantContactIds = memberKeys.map { memberKey -> memberKey.contactId },
                    recipientStates = recipientStates,
                    invitations = invitations
                )
        }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val RECENT_MESSAGE_LIMIT = 500
    }
}
