package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class LeaveGroupUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEnd> = repository.leave(groupId, context)
}
