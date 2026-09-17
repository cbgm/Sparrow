package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.sparrow.data.database.entity.GroupMemberKeyEntity
import com.cbgm.sparrow.feature.membership.data.model.CreatedGroupSecurityDto
import com.cbgm.sparrow.feature.membership.data.model.GroupWelcomeRecipientDto

interface GroupMembershipSecurityDataSource {
    suspend fun findOwnedGroupEpoch(groupId: String): Result<Int?>

    suspend fun findCurrentEpoch(groupId: String): Result<Int?>

    suspend fun findLocalRole(groupId: String): Result<String?>

    suspend fun clearRetiredMembershipBeforeRejoin(groupId: String): Result<Unit>

    suspend fun findRemoteMemberKey(
        groupId: String,
        contactId: String
    ): Result<GroupMemberKeyEntity?>

    suspend fun createOwnedGroup(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        memberPayloads: List<GroupMemberPayload>,
        memberKeys: List<GroupMemberKeyEntity>,
        recipients: List<GroupWelcomeRecipientDto>,
        localSigningKeyPair: LocalSigningKeyPair
    ): Result<CreatedGroupSecurityDto>

    suspend fun rotateOwnedGroup(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long,
        memberPayloads: List<GroupMemberPayload>,
        memberKeys: List<GroupMemberKeyEntity>,
        recipients: List<GroupWelcomeRecipientDto>,
        localSigningKeyPair: LocalSigningKeyPair,
        membershipChange: GroupMembershipChangePayload? = null
    ): Result<CreatedGroupSecurityDto>

    fun welcomePacketId(
        groupId: String,
        invitationId: String,
        epoch: Int
    ): String

    suspend fun verifyKeyConfirmation(
        groupId: String,
        epoch: Int,
        keyConfirmation: ByteArray
    ): Result<Unit>
}
