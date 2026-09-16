package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class AddGroupMembersUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit> =
        repository.addMembers(groupId, contactIds)
}
