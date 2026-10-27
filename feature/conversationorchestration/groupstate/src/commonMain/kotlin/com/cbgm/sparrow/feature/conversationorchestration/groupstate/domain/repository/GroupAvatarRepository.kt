package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState

interface GroupAvatarRepository {
    suspend fun get(groupId: String): Result<GroupAvatarState>

    suspend fun save(state: GroupAvatarState): Result<Unit>

    /** Clears the unpublished mark only while [changedAtEpochMilliseconds] is still the stored version. */
    suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long): Result<Unit>

    /** Groups whose stored version was saved locally but not queued for the members yet. */
    suspend fun findPublishPendingGroupIds(): Result<List<String>>

    suspend fun deleteLocal(groupId: String): Result<Unit>
}
