package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupConversationUseCase(
    private val repository: GroupConversationRepository
) {
    operator fun invoke(groupId: String): Flow<GroupConversation?> = repository.observe(groupId)
}
