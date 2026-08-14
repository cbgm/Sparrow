package com.cbgm.sparrow.feature.chats.domain.repository.group

interface GroupVerificationActionRepository {
    suspend fun synchronize(groupId: String): Result<Unit>

    suspend fun verify(
        groupId: String,
        contactId: String
    ): Result<Unit>
}
