package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupLocalCleanupDataSource
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupLocalConversationRepository

internal class GroupLocalConversationRepositoryImpl(
    private val dataSource: GroupLocalCleanupDataSource
) : GroupLocalConversationRepository {
    override suspend fun endMembership(
        groupId: String,
        referenceId: String,
        epoch: Int,
        endedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        dataSource.endMembership(groupId, referenceId, epoch, endedAtEpochMilliseconds)
    }

    override suspend fun deleteConversation(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    ): Result<Unit> = runCatching {
        dataSource.deleteConversationHistory(groupId, deletedAtEpochMilliseconds)
    }
}
