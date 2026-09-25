package com.cbgm.sparrow.feature.chats.data.group.datasource

import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.dao.GroupPinDao
import com.cbgm.sparrow.data.database.entity.GroupPinEntity
import com.cbgm.sparrow.data.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

internal class GroupPinDataSource(
    private val groupPinDao: GroupPinDao,
    private val chatDao: ChatDao
) {
    fun observe(groupId: String): Flow<GroupPinEntity?> = groupPinDao.observe(groupId)

    suspend fun findMessage(messageId: String): MessageEntity? = chatDao.findMessageById(messageId)

    suspend fun get(groupId: String): GroupPinEntity? = groupPinDao.get(groupId)

    suspend fun save(entity: GroupPinEntity) {
        groupPinDao.upsert(entity)
    }

    suspend fun delete(groupId: String) {
        groupPinDao.delete(groupId)
    }
}
