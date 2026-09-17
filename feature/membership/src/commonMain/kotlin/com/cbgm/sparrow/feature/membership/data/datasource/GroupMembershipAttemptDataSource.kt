package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.feature.membership.data.model.GroupMembershipAttemptDto

interface GroupMembershipAttemptDataSource {
    suspend fun initializeOwnedGroup(groupId: String): Result<Unit>

    suspend fun findBySourceInvitationId(invitationId: String): GroupMembershipAttemptDto?

    suspend fun findOwnerAttempt(
        groupId: String,
        contactId: String
    ): GroupMembershipAttemptDto?

    suspend fun stageOwnerAttempt(
        groupId: String,
        contactId: String,
        sourceInvitationId: String,
        challenge: ByteArray,
        createdAtEpochMilliseconds: Long
    ): Result<GroupMembershipAttemptDto>

    suspend fun stageMemberAttempt(
        groupId: String,
        ownerContactId: String,
        sourceInvitationId: String,
        challenge: ByteArray,
        ownerEncryptionPublicKey: ByteArray,
        ownerSigningPublicKey: ByteArray,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long
    ): Result<GroupMembershipAttemptDto>

    suspend fun markJoinRequested(
        sourceInvitationId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<GroupMembershipAttemptDto>

    suspend fun markJoinSendFailed(
        sourceInvitationId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<GroupMembershipAttemptDto>

    suspend fun markIdentityConfirmed(
        sourceInvitationId: String,
        updatedAtEpochMilliseconds: Long
    ): Result<GroupMembershipAttemptDto>

    suspend fun deleteAttempt(sourceInvitationId: String): Result<Unit>

    suspend fun deleteOwnerAttempt(
        groupId: String,
        contactId: String
    ): Result<Unit>

    suspend fun deleteSupersededMemberStagedAttempts(
        ownerContactId: String,
        currentInvitationId: String
    ): Result<Unit>

    suspend fun refreshOwnedMembership(groupId: String): Result<Unit>

    suspend fun clearRetiredMembershipBeforeRejoin(groupId: String): Result<Unit>
}
