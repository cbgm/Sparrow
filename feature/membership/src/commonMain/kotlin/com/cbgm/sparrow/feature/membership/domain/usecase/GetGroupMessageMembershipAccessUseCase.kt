package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupMessageMembershipAccess
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

class GetGroupMessageMembershipAccessUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String): Result<GroupMessageMembershipAccess> =
        repository.inspectMessageAccess(groupId)
}
