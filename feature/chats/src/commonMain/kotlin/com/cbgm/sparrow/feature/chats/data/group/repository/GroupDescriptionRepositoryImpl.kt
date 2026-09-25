package com.cbgm.sparrow.feature.chats.data.group.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupDescriptionDataSource
import com.cbgm.sparrow.feature.chats.data.group.description.GroupDescriptionBroadcaster
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupDescription
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupDescriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class GroupDescriptionRepositoryImpl(
    private val dataSource: GroupDescriptionDataSource,
    private val broadcaster: GroupDescriptionBroadcaster
) : GroupDescriptionRepository {
    private val updateMutex = Mutex()

    override fun observe(groupId: String): Flow<GroupDescription> = dataSource.observe(groupId)

    override suspend fun set(
        groupId: String,
        description: String
    ): Result<Unit> =
        safeSuspendCall {
            require(groupId.isNotBlank()) { "Group ID must not be blank" }
            val normalized = description.trim().takeIf(String::isNotEmpty)
            require(normalized == null || normalized.length <= GroupDescription.MAX_LENGTH) {
                "Group description must not exceed ${GroupDescription.MAX_LENGTH} characters"
            }

            updateMutex.withLock {
                broadcaster.requireLocalAdmin(groupId).getOrThrow()
                val previous = dataSource.get(groupId)
                if (previous.description == normalized) return@withLock

                val changedAt =
                    maxOf(
                        SystemClock.nowEpochMilliseconds(),
                        previous.changedAtEpochMilliseconds + 1L
                    )
                dataSource.save(
                    groupId = groupId,
                    description = normalized,
                    changedAtEpochMilliseconds = changedAt
                )
                broadcaster.broadcast(groupId).getOrThrow()
            }
        }
}
