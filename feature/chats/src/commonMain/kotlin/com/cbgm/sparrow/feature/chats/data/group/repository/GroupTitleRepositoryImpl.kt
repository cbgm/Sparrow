package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupTitleDataSource
import com.cbgm.sparrow.feature.chats.data.group.title.GroupTitleBroadcaster
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupTitleRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class GroupTitleRepositoryImpl(
    private val dataSource: GroupTitleDataSource,
    private val broadcaster: GroupTitleBroadcaster
) : GroupTitleRepository {
    private val updateMutex = Mutex()

    override suspend fun set(
        groupId: String,
        title: String
    ): Result<Unit> =
        safeSuspendCall {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotEmpty()) { "Group title must not be blank" }

            updateMutex.withLock {
                broadcaster.requireLocalAdmin(groupId).getOrThrow()
                val previous = dataSource.get(groupId)
                if (previous.title == normalizedTitle) return@withLock

                val changedAt =
                    maxOf(
                        SystemClock.nowEpochMilliseconds(),
                        previous.changedAtEpochMilliseconds + 1L
                    )
                dataSource.save(
                    groupId = groupId,
                    title = normalizedTitle,
                    changedAtEpochMilliseconds = changedAt
                )
                broadcaster.broadcast(groupId).getOrThrow()
            }
        }
}
