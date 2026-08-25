package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.chats.data.group.avatar.GroupAvatarBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarDataSource
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class GroupAvatarRepositoryImpl(
    private val dataSource: GroupAvatarDataSource,
    private val broadcaster: GroupAvatarBroadcaster
) : GroupAvatarRepository {
    private val updateMutex = Mutex()

    override fun observe(groupId: String): Flow<GroupAvatar> = dataSource.observe(groupId)

    override suspend fun set(
        groupId: String,
        bytes: ByteArray
    ): Result<Unit> =
        update(groupId, bytes)

    override suspend fun remove(groupId: String): Result<Unit> =
        update(groupId, null)

    private suspend fun update(
        groupId: String,
        bytes: ByteArray?
    ): Result<Unit> =
        runCatching {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            bytes?.let { avatarBytes ->
                require(avatarBytes.isNotEmpty()) { "Group avatar must not be empty" }
                require(avatarBytes.size <= MAX_GROUP_AVATAR_BYTES) {
                    "Group avatar must not exceed $MAX_GROUP_AVATAR_BYTES bytes"
                }
            }

            updateMutex.withLock {
                broadcaster.requireLocalAdmin(groupId).getOrThrow()
                val previous = dataSource.get(groupId)
                val changedAt =
                    maxOf(
                        SystemClock.nowEpochMilliseconds(),
                        previous.changedAtEpochMilliseconds + 1L
                    )
                if (bytes == null) {
                    dataSource.remove(groupId, changedAt)
                } else {
                    dataSource.save(groupId, bytes, changedAt)
                }
                broadcaster.broadcast(groupId).getOrThrow()
            }
        }

    private companion object {
        const val MAX_GROUP_AVATAR_BYTES = 1_048_576
    }
}
