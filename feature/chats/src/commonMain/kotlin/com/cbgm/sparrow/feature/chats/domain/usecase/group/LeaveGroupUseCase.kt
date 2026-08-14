package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMembershipRepository

class LeaveGroupUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = repository.leave(groupId)
}
