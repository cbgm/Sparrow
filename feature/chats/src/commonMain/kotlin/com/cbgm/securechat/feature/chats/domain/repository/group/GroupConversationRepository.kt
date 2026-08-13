package com.cbgm.securechat.feature.chats.domain.repository.group

import com.cbgm.securechat.feature.chats.domain.model.group.GroupConversation
import kotlinx.coroutines.flow.Flow

interface GroupConversationRepository {
    fun observe(groupId: String): Flow<GroupConversation?>
}
