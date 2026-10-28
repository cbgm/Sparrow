package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

/** The group's installed epoch, or null before its first welcome. */
class FindGroupCurrentEpochUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String): Result<Int?> = repository.findCurrentEpoch(groupId)
}
