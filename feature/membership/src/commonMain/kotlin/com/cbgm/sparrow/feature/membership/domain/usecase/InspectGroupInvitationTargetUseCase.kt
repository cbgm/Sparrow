package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupInvitationTarget
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

/** What the device already holds for the group an incoming invitation names. */
class InspectGroupInvitationTargetUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String): Result<GroupInvitationTarget> =
        repository.inspectInvitationTarget(groupId)
}
