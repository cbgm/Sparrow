package com.cbgm.sparrow.feature.chats.data.group.verification

import com.cbgm.sparrow.data.database.dao.GroupVerificationDao
import com.cbgm.sparrow.data.database.entity.GroupVerificationPairEntity

/** Chats owns verification rows; only this datasource talks to their DAO. */
internal class GroupVerificationDataSource(
    private val dao: GroupVerificationDao
) {
    suspend fun findByGroupId(groupId: String): List<GroupVerificationPairEntity> = dao.findByGroupId(groupId)

    suspend fun findPair(groupId: String, invitationId: String): GroupVerificationPairEntity? =
        dao.findPair(groupId, invitationId)

    suspend fun findLatestUpdatedAt(groupId: String): Long? = dao.findLatestUpdatedAt(groupId)

    suspend fun replaceGroup(groupId: String, rows: List<GroupVerificationPairEntity>) =
        dao.replaceGroup(groupId, rows)

    suspend fun markAdminVerifiedParticipant(groupId: String, invitationId: String, updatedAt: Long): Int =
        dao.markAdminVerifiedParticipant(groupId, invitationId, updatedAt)

    suspend fun markParticipantVerifiedAdmin(groupId: String, invitationId: String, updatedAt: Long): Int =
        dao.markParticipantVerifiedAdmin(groupId, invitationId, updatedAt)
}
