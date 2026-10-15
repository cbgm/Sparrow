package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.GroupPinDao
import com.cbgm.sparrow.data.database.entity.GroupPinEntity
import kotlinx.coroutines.flow.Flow

internal class GroupPinDataSource(
    private val groupPinDao: GroupPinDao
) {
    fun observe(groupId: String): Flow<GroupPinEntity?> = groupPinDao.observe(groupId)

    suspend fun get(groupId: String): GroupPinEntity? = groupPinDao.get(groupId)
}
