package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository

import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationRow

interface GroupVerificationRepository {
    suspend fun findRows(groupId: String): Result<List<GroupVerificationRow>>

    suspend fun findRow(groupId: String, invitationId: String): Result<GroupVerificationRow?>

    suspend fun findLatestUpdatedAt(groupId: String): Result<Long?>

    suspend fun replaceGroup(groupId: String, rows: List<GroupVerificationRow>): Result<Unit>

    /** False when the row is no longer an active verification target. */
    suspend fun markAdminVerifiedParticipant(
        groupId: String,
        invitationId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean>

    /** False when the row is no longer an active verification target. */
    suspend fun markParticipantVerifiedAdmin(
        groupId: String,
        invitationId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean>
}
