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
            changedAtEpochMilliseconds = metadata?.descriptionChangedAtEpochMilliseconds ?: 0L,
            publishPending = metadata?.descriptionPublishPending ?: false
        )
    }

    suspend fun save(groupId: String, description: GroupDescriptionDto) {
        groupMetadataDao.upsertDescription(
            groupId = groupId,
            description = description.description,
            changedAtEpochMilliseconds = description.changedAtEpochMilliseconds,
            publishPending = description.publishPending
        )
    }

    suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long) {
        groupMetadataDao.markDescriptionPublished(groupId, changedAtEpochMilliseconds)
    }

    suspend fun findPublishPendingGroupIds(): List<String> = groupMetadataDao.findDescriptionPublishPendingGroupIds()

    suspend fun deleteLocal(groupId: String) {
        groupMetadataDao.clearDescription(groupId)
    }
}
