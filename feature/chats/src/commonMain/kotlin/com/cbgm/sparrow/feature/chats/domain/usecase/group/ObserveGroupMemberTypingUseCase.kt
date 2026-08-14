package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupTypingRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupMemberTypingUseCase(
    private val repository: GroupTypingRepository
) {
    operator fun invoke(
        groupId: String,
        contactId: String
    ): Flow<Boolean> = repository.observeMember(groupId, contactId)
}
