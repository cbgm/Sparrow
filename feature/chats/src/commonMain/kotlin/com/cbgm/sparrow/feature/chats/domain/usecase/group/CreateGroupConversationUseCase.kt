package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMembershipRepository

class CreateGroupConversationUseCase(
    private val repository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        title: String,
        contactIds: Set<String>
    ): Result<String> =
        repository.create(title, contactIds)
}
