package com.cbgm.securechat.feature.chats.domain.usecase.group

import com.cbgm.securechat.feature.chats.domain.repository.group.GroupVerificationActionRepository

class VerifyGroupMemberUseCase(
    private val repository: GroupVerificationActionRepository
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        repository.verify(groupId, contactId)
}
