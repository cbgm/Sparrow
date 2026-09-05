package com.cbgm.sparrow.feature.chats.data.group.datasource

interface GroupKeyDataSource {
    suspend fun save(
        groupId: String,
        epoch: Int,
        groupKey: ByteArray
    )

    suspend fun load(
        groupId: String,
        epoch: Int
    ): ByteArray?

    suspend fun deleteBefore(
        groupId: String,
        epoch: Int
    )

    suspend fun deleteGroup(groupId: String)
}
