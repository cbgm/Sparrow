package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class InitializeOwnedGroupMembershipUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> =
        repository.initializeOwnedGroup(groupId)
}
