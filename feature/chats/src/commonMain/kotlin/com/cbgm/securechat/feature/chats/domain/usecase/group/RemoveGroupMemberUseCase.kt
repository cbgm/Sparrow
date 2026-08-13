package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.repository.group.GroupMembershipRepository

class RemoveGroupMemberUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        repository.removeMember(groupId, contactId)
}
