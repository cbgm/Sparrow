package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import kotlinx.coroutines.flow.Flow

interface GroupConversationReadRepository {
    fun observe(
        groupId: String,
        oldestCursor: MessageHistoryCursor? = null
    ): Flow<GroupConversation?>
}
