package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.identity.LocalPublicIdentity
import com.cbgm.sparrow.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberPayload
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket

interface GroupMembershipProtocolDataSource {
    suspend fun createConversationDeleted(
        invitationId: String,
        groupId: String,
        epoch: Int,
        challenge: ByteArray,
        deletedAtEpochMilliseconds: Long,
        ownerSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupConversationDeletedPacket>

    suspend fun createInvite(
        invitationId: String,
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        expiresAtEpochMilliseconds: Long,
        ownerIdentity: LocalPublicIdentity,
        ownerSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupInvitePacket>

    suspend fun verifyInvite(packet: GroupInvitePacket): Result<Unit>

    suspend fun createInviteReceived(
        invite: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long,
        memberSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupInviteReceivedPacket>

    suspend fun verifyInviteReceived(packet: GroupInviteReceivedPacket): Result<Unit>

    suspend fun createJoinRequest(
        invitationId: String,
        groupId: String,
        challenge: ByteArray,
        memberIdentity: LocalPublicIdentity,
        memberSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupJoinRequestPacket>

    suspend fun verifyJoinRequest(packet: GroupJoinRequestPacket): Result<Unit>

    suspend fun createDecline(
        invitationId: String,
        groupId: String,
        challenge: ByteArray,
        memberSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupInviteDeclinedPacket>

    suspend fun verifyDecline(packet: GroupInviteDeclinedPacket): Result<Unit>

    suspend fun createLeaveRequest(
        invitationId: String,
        groupId: String,
        epoch: Int,
        challenge: ByteArray,
        requestedAtEpochMilliseconds: Long,
        memberSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupLeaveRequestPacket>

    suspend fun verifyLeaveRequest(
        packet: GroupLeaveRequestPacket,
        expectedMemberSigningPublicKey: ByteArray
    ): Result<Unit>

    suspend fun verifyReadyAcknowledgement(
        packet: GroupReadyAcknowledgementPacket,
        expectedMemberSigningPublicKey: ByteArray
    ): Result<Unit>

    suspend fun createMemberActivated(
        groupId: String,
        epoch: Int,
        member: GroupMemberPayload,
        activatedAtEpochMilliseconds: Long,
        activationRound: Int,
        activationId: String,
        memberReferenceId: String,
        recipientContactId: String,
        ownerSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupMemberActivatedPacket>

    suspend fun createMemberRemoved(
        invitationId: String,
        groupId: String,
        epoch: Int,
        reason: String = GroupMemberRemovedPacket.REASON_REMOVED_BY_OWNER,
        challenge: ByteArray,
        removedMemberSigningPublicKey: ByteArray,
        removedAtEpochMilliseconds: Long,
        ownerSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupMemberRemovedPacket>
}
