package com.cbgm.sparrow.feature.chats.data.overview.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.feature.chats.data.group.mapper.GroupMembershipMessageFactory
import com.cbgm.sparrow.feature.chats.data.overview.mapper.toConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversationOverviewRepositoryImpl(
    private val chatDao: ChatDao
) : ConversationOverviewRepository {
    override fun observeAll(): Flow<List<ConversationOverview>> =
        chatDao.observeConversationSummaries(
            localDeletionTransportMode = GroupMembershipMessageFactory.LOCAL_CONVERSATION_DELETED_TRANSPORT_MODE,
            localMembershipStartedTransportMode = GroupMembershipMessageFactory.LOCAL_MEMBERSHIP_STARTED_TRANSPORT_MODE
        ).map { summaries -> summaries.map { it.toConversationOverview() } }

    override suspend fun incrementUnseenLocalMessageCount(conversationId: String): Result<Unit> =
        safeSuspendCall {
            check(chatDao.incrementUnseenLocalMessageCount(conversationId) == 1) {
                "Conversation not found"
            }
        }

    override suspend fun clearUnseenLocalMessageCount(conversationId: String): Result<Unit> =
        safeSuspendCall {
            check(chatDao.clearUnseenLocalMessageCount(conversationId) == 1) {
                "Conversation not found"
            }
        }
}
