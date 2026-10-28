package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.feature.membership.domain.model.GroupCommittedMemberRemoval
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository

/** The removal of [contactId] the local admin already committed, or null when the member still has to be removed. */
class FindCommittedGroupMemberRemovalUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(groupId: String, contactId: String): Result<GroupCommittedMemberRemoval?> =
        repository.findCommittedRemoval(groupId, contactId)
}
