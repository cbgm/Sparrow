package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.repository.group.GroupTypingRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupMemberTypingUseCase(
    private val repository: GroupTypingRepository
) {
    operator fun invoke(contactId: String): Flow<Boolean> = repository.observeMember(contactId)
}
