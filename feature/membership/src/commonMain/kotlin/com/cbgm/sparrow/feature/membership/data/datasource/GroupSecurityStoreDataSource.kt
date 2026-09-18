package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.data.database.dao.GroupSecurityDao
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.data.database.entity.GroupSecurityStateEntity
import kotlinx.coroutines.flow.Flow

internal class GroupSecurityStoreDataSource(
    private val dao: GroupSecurityDao
) {
    suspend fun findState(groupId: String): GroupSecurityStateEntity? = dao.findState(groupId)

    suspend fun findMemberKeys(
        groupId: String,
        epoch: Int
    ): List<GroupMemberKeyEntity> = dao.findMemberKeys(groupId, epoch)

    suspend fun findMemberKey(
        groupId: String,
        epoch: Int,
        contactId: String
    ): GroupMemberKeyEntity? = dao.findMemberKey(groupId, epoch, contactId)

    fun observeState(groupId: String): Flow<GroupSecurityStateEntity?> = dao.observeState(groupId)

    fun observeCurrentMemberKeys(groupId: String): Flow<List<GroupMemberKeyEntity>> =
        dao.observeCurrentMemberKeys(groupId)
}
