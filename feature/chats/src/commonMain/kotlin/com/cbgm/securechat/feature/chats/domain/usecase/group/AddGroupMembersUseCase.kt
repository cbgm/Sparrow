package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.repository.group.GroupMembershipRepository

class AddGroupMembersUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        contactIds: Set<String>
    ): Result<Unit> =
        repository.addMembers(groupId, contactIds)
}
