package com.cbgm.sparrow.feature.chats.domain.repository.group

interface GroupTitleRepository {
    suspend fun set(
        groupId: String,
        title: String
    ): Result<Unit>

    suspend fun sendCurrentTo(groupId: String, peerId: String): Result<Unit>
}
