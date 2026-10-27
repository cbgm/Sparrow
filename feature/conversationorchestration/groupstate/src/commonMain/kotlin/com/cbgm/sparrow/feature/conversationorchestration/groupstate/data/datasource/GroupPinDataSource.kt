package com.cbgm.sparrow.feature.conversationorchestration.groupstate.data.datasource

import com.cbgm.sparrow.data.database.dao.GroupPinDao
import com.cbgm.sparrow.data.database.entity.GroupPinEntity

internal class GroupPinDataSource(
    private val groupPinDao: GroupPinDao
) {
    suspend fun get(groupId: String): GroupPinEntity? = groupPinDao.get(groupId)

    suspend fun save(entity: GroupPinEntity) {
        groupPinDao.upsert(entity)
    }

    suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long) {
        groupPinDao.markPublished(groupId, changedAtEpochMilliseconds)
    }

    suspend fun findPublishPendingGroupIds(): List<String> = groupPinDao.findPublishPendingGroupIds()

    suspend fun delete(groupId: String) {
        groupPinDao.delete(groupId)
    }
}
