package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupDescriptionState

interface GroupDescriptionRepository {
    suspend fun get(groupId: String): Result<GroupDescriptionState>

    /**
     * Saves [state] only while [expectedChangedAtEpochMilliseconds] is still the stored version, atomically against
     * other saves; false when another version was stored meanwhile.
     */
    suspend fun saveIfVersion(state: GroupDescriptionState, expectedChangedAtEpochMilliseconds: Long): Result<Boolean>

    /** Clears the unpublished mark only while [changedAtEpochMilliseconds] is still the stored version. */
    suspend fun markPublished(groupId: String, changedAtEpochMilliseconds: Long): Result<Unit>

    /** Groups whose stored version was saved locally but not queued for the members yet. */
    suspend fun findPublishPendingGroupIds(): Result<List<String>>

    suspend fun deleteLocal(groupId: String): Result<Unit>
}
