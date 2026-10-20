package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationReadRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupConversationUseCase(
    private val repository: GroupConversationReadRepository
) {
    operator fun invoke(groupId: String): Flow<GroupConversation?> = repository.observe(groupId)
}
