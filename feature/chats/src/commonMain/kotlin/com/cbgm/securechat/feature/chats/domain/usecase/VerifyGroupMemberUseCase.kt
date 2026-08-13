package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.GroupVerificationActionRepository

class VerifyGroupMemberUseCase(
    private val repository: GroupVerificationActionRepository
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        repository.verify(
            groupId = groupId,
            contactId = contactId
        )
}
