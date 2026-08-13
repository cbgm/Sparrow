package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.model.group.GroupLeaveRequirement
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupMembershipRepository

class GetGroupLeaveRequirementUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String
    ): Result<GroupLeaveRequirement> =
        repository.getLeaveRequirement(groupId)
}
