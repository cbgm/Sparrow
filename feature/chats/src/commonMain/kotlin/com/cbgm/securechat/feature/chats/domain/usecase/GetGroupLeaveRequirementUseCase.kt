package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.model.GroupLeaveRequirement
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository

class GetGroupLeaveRequirementUseCase(
    private val repository: ChatsRepository
) {
    suspend operator fun invoke(groupId: String): Result<GroupLeaveRequirement> =
        repository.getGroupLeaveRequirement(groupId)
}
