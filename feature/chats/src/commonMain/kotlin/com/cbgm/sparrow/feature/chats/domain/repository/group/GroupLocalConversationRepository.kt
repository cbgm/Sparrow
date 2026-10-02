package com.cbgm.sparrow.feature.chats.domain.repository.group

interface GroupLocalConversationRepository {
    suspend fun endMembership(
        groupId: String,
        referenceId: String,
        epoch: Int,
        endedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun deleteConversation(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ): Result<Unit>
}
