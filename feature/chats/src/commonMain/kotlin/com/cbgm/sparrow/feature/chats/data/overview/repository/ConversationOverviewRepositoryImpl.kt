package com.cbgm.sparrow.feature.chats.data.overview.repository

import com.cbgm.sparrow.core.base.message.domain.model.MessageTransportMode
import com.cbgm.sparrow.feature.chats.data.overview.datasource.ConversationOverviewDataSource
import com.cbgm.sparrow.feature.chats.data.overview.mapper.toConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversationOverviewRepositoryImpl internal constructor(
    private val dataSource: ConversationOverviewDataSource
) : ConversationOverviewRepository {
    override fun observeAll(): Flow<List<ConversationOverview>> =
        dataSource.observeSummaries(
            localDeletionTransportMode = MessageTransportMode.LOCAL_CONVERSATION_DELETED,
            localMembershipStartedTransportMode = MessageTransportMode.LOCAL_GROUP_MEMBERSHIP_STARTED
        ).map { summaries -> summaries.map { it.toConversationOverview() } }
}
