package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupKeyDataSource
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupKeyRepository

class GroupKeyRepositoryImpl(
    private val dataSource: GroupKeyDataSource
) : GroupKeyRepository {
    override suspend fun save(
        groupId: String,
        epoch: Int,
        groupKey: ByteArray
    ): Result<Unit> = dataSource.save(groupId, epoch, groupKey)

    override suspend fun load(
        groupId: String,
        epoch: Int
    ): Result<ByteArray?> = dataSource.load(groupId, epoch)

    override suspend fun deleteBefore(
        groupId: String,
        epoch: Int
    ): Result<Unit> = dataSource.deleteBefore(groupId, epoch)

    override suspend fun deleteGroup(groupId: String): Result<Unit> =
        dataSource.deleteGroup(groupId)
}
