package com.cbgm.sparrow.feature.conversationorchestration.membership.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.datasource.GroupVerificationDataSource
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper.toGroupVerificationPairEntity
import com.cbgm.sparrow.feature.conversationorchestration.membership.data.mapper.toGroupVerificationRow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationRow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupVerificationRepository

internal class GroupVerificationRepositoryImpl(
    private val dataSource: GroupVerificationDataSource
) : GroupVerificationRepository {
    override suspend fun findRows(groupId: String): Result<List<GroupVerificationRow>> =
        safeSuspendCall { dataSource.findByGroupId(groupId).map { it.toGroupVerificationRow() } }

    override suspend fun findRow(groupId: String, invitationId: String): Result<GroupVerificationRow?> =
        safeSuspendCall { dataSource.findPair(groupId, invitationId)?.toGroupVerificationRow() }

    override suspend fun findLatestUpdatedAt(groupId: String): Result<Long?> =
        safeSuspendCall { dataSource.findLatestUpdatedAt(groupId) }

    override suspend fun replaceGroup(groupId: String, rows: List<GroupVerificationRow>): Result<Unit> =
        safeSuspendCall { dataSource.replaceGroup(groupId, rows.map { it.toGroupVerificationPairEntity() }) }

    override suspend fun markAdminVerifiedParticipant(
        groupId: String,
        invitationId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean> =
        safeSuspendCall {
            dataSource.markAdminVerifiedParticipant(groupId, invitationId, updatedAtEpochMilliseconds) == 1
        }

    override suspend fun markParticipantVerifiedAdmin(
        groupId: String,
        invitationId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Boolean> =
        safeSuspendCall {
            dataSource.markParticipantVerifiedAdmin(groupId, invitationId, updatedAtEpochMilliseconds) == 1
        }
}
