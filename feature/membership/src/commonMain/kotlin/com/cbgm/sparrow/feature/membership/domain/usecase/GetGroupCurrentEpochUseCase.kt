package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class GetGroupCurrentEpochUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String): Result<Int> = repository.getCurrentEpoch(groupId)
}
