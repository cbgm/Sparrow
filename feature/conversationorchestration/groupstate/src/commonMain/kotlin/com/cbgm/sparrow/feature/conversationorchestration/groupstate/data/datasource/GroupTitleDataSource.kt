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
            changedAtEpochMilliseconds = metadata.titleChangedAtEpochMilliseconds
        )
    }

    suspend fun save(groupId: String, title: GroupTitleDto) {
        groupMetadataDao.upsertTitle(groupId, title.title, title.changedAtEpochMilliseconds)
    }

    suspend fun deleteLocal(groupId: String) {
        groupMetadataDao.clearTitleChangedAt(groupId)
    }
}
