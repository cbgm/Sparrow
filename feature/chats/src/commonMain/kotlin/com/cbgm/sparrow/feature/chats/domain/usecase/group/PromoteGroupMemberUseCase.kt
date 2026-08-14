package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMembershipRepository

class PromoteGroupMemberUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        repository.promoteMember(groupId, contactId)
}
