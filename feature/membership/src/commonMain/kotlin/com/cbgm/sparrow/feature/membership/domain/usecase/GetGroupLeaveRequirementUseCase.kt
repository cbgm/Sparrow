package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class GetGroupLeaveRequirementUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String
    ): Result<GroupLeaveRequirement> =
        repository.getLeaveRequirement(groupId)
}
