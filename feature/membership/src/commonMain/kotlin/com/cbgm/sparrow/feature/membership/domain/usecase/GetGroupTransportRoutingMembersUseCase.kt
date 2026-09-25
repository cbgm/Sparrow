package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupTransportRoutingMember
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

/** Membership exposes installed signing keys, not derived transport routing IDs. */
class GetGroupTransportRoutingMembersUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String): Result<List<GroupTransportRoutingMember>?> =
        repository.getCurrentTransportRoutingMembers(groupId)

    suspend fun allCurrent(): Result<List<GroupTransportRoutingMember>> =
        repository.getAllCurrentTransportRoutingMembers()
}
