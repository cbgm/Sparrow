package com.cbgm.sparrow.feature.chats.data.group.datasource

interface GroupKeyDataSource {
    suspend fun save(
        groupId: String,
        epoch: Int,
        groupKey: ByteArray
    ): Result<Unit>

    suspend fun load(
        groupId: String,
        epoch: Int
    ): Result<ByteArray?>

    suspend fun deleteBefore(
        groupId: String,
        epoch: Int
    ): Result<Unit>

    suspend fun deleteGroup(groupId: String): Result<Unit>
}
