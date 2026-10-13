package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource

import com.cbgm.sparrow.data.database.dao.GroupMetadataDao
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.model.GroupDescriptionDto

internal class GroupDescriptionDataSource(
    private val groupMetadataDao: GroupMetadataDao
) {
    suspend fun get(groupId: String): GroupDescriptionDto {
        val metadata = groupMetadataDao.get(groupId)
        return GroupDescriptionDto(
            description = metadata?.description,
            changedAtEpochMilliseconds = metadata?.descriptionChangedAtEpochMilliseconds ?: 0L
        )
    }

    suspend fun save(groupId: String, description: GroupDescriptionDto) {
        groupMetadataDao.upsertDescription(
            groupId = groupId,
            description = description.description,
            changedAtEpochMilliseconds = description.changedAtEpochMilliseconds
        )
    }

    suspend fun deleteLocal(groupId: String) {
        groupMetadataDao.clearDescription(groupId)
    }
}
