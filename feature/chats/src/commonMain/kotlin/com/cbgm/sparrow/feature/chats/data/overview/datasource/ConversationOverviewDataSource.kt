package com.cbgm.sparrow.feature.chats.data.overview.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.model.ConversationSummaryDto
import kotlinx.coroutines.flow.Flow

/** All conversation-overview database access stays in the Chats data layer. */
internal class ConversationOverviewDataSource(
    private val chatDao: ChatDao
) {
    fun observeSummaries(
        localDeletionTransportMode: String,
        localMembershipStartedTransportMode: String
    ): Flow<List<ConversationSummaryDto>> =
        chatDao.observeConversationSummaries(
            localDeletionTransportMode = localDeletionTransportMode,
            localMembershipStartedTransportMode = localMembershipStartedTransportMode
        )
}
