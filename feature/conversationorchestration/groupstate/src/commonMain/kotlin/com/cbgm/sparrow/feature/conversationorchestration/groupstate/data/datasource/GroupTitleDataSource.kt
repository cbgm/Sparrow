package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource

import com.cbgm.sparrow.data.database.dao.GroupMetadataDao
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model.GroupTitleDto

internal class GroupTitleDataSource(
    private val groupMetadataDao: GroupMetadataDao
) {
    suspend fun get(groupId: String): GroupTitleDto {
        val metadata = groupMetadataDao.get(groupId) ?: error("Group conversation was not found")
        return GroupTitleDto(
            title = metadata.title,
            changedAtEpochMilliseconds = metadata.titleChangedAtEpochMilliseconds,
            publishPending = metadata.titlePublishPending
        )
    }

    suspend fun saveIfVersion(
        groupId: String,
        title: GroupTitleDto,
        expectedChangedAtEpochMilliseconds: Long
    ): Boolean =
        groupMetadataDao.replaceTitleIfVersion(
            groupId = groupId,
            expectedChangedAtEpochMilliseconds = expectedChangedAtEpochMilliseconds,
            title = title.title,
            changedAtEpochMilliseconds = title.changedAtEpochMilliseconds,
            publishPending = title.publishPending
        )

    suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long) {
        groupMetadataDao.markTitlePublished(groupId, changedAtEpochMilliseconds)
    }

    suspend fun findPublishPendingGroupIds(): List<String> = groupMetadataDao.findTitlePublishPendingGroupIds()

    suspend fun deleteLocal(groupId: String) {
        groupMetadataDao.clearTitleChangedAt(groupId)
    }
}
