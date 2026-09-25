package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class RemoveGroupMemberUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> =
        repository.removeMember(groupId, contactId, context)
}
