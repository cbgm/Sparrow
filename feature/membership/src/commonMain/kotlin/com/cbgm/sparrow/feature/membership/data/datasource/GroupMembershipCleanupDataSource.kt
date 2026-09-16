package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.data.database.entity.MessageEntity

interface GroupMembershipCleanupDataSource {
    suspend fun endMembership(message: MessageEntity)

    suspend fun deleteConversationHistory(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    )

    suspend fun delete(
        groupId: String,
        deletedAtEpochMilliseconds: Long
    )
}
