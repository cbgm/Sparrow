package com.cbgm.securechat.feature.chats.data.overview.repository

import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.securechat.feature.chats.data.overview.mapper.toConversationOverview
import com.cbgm.securechat.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.securechat.feature.chats.domain.repository.overview.ConversationOverviewRepository
import com.cbgm.securechat.feature.contacts.domain.model.IdentityHandshakeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversationOverviewRepositoryImpl(
    private val chatDao: ChatDao
) : ConversationOverviewRepository {
    override fun observeAll(): Flow<List<ConversationOverview>> =
        chatDao.observeConversationSummaries(
            localDeletionTransportMode = GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE,
            localMembershipStartedTransportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_STARTED_TRANSPORT_MODE,
            directChatAuthorizedState = IdentityHandshakeState.MUTUAL_UNVERIFIED.name,
            directChatDeletedState = IdentityHandshakeState.CONVERSATION_DELETED.name
        ).map { summaries -> summaries.map { it.toConversationOverview() } }
}
