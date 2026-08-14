package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.model.group.GroupConversation
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupConversationRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupConversationUseCase(
    private val repository: GroupConversationRepository
) {
    operator fun invoke(groupId: String): Flow<GroupConversation?> = repository.observe(groupId)
}
