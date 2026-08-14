package com.cbgm.securechat.feature.chats.data.group.repository

import com.cbgm.securechat.feature.chats.data.group.storage.AndroidGroupKeyStorage
import com.cbgm.securechat.feature.chats.domain.repository.group.GroupKeyRepository

class GroupKeyRepositoryImpl(
    private val storage: AndroidGroupKeyStorage
) : GroupKeyRepository {
    override suspend fun save(
        groupId: String,
        epoch: Int,
        groupKey: ByteArray
    ): Result<Unit> = storage.save(groupId, epoch, groupKey)

    override suspend fun load(
        groupId: String,
        epoch: Int
    ): Result<ByteArray?> = storage.load(groupId, epoch)

    override suspend fun deleteBefore(
        groupId: String,
        epoch: Int
    ): Result<Unit> = storage.deleteBefore(groupId, epoch)

    override suspend fun deleteGroup(groupId: String): Result<Unit> =
        storage.deleteGroup(groupId)
}
