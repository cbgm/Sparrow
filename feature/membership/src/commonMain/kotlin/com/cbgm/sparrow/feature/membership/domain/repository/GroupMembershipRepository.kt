package com.cbgm.sparrow.feature.membership.domain.repository

import com.cbgm.sparrow.core.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.feature.membership.domain.model.GroupAdministrationState
import com.cbgm.sparrow.feature.membership.domain.model.GroupConversationMembershipSnapshot
import com.cbgm.sparrow.feature.membership.domain.model.GroupIncomingWelcomeAuthorization
import com.cbgm.sparrow.feature.membership.domain.model.GroupLeaveRequirement
import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberPromotionResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.GroupMessageMembershipAccess
import com.cbgm.sparrow.feature.membership.domain.model.GroupMetadataMessageSender
import com.cbgm.sparrow.feature.membership.domain.model.GroupMetadataSendContext
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.MembershipVerificationSnapshot
import kotlinx.coroutines.flow.Flow

interface GroupMembershipRepository {
    /** One coherent Membership-owned read for group-verification checks/projection. */
    suspend fun getVerificationMembershipContext(groupId: String): Result<GroupVerificationMembershipContext>

    suspend fun authorizeIncomingWelcome(
        groupId: String,
        senderContactId: String,
        packetId: String,
        epoch: Int
    ): Result<GroupIncomingWelcomeAuthorization?>

    /** Sends an acknowledged welcome using Membership's installed epoch key and packet encoder. */
    suspend fun sendGroupReadyAcknowledgement(
        groupId: String,
        epoch: Int,
        welcomePacketId: String,
        recipientContactId: String
    ): Result<Unit>

    suspend fun completeIncomingWelcome(
        groupId: String,
        senderContactId: String,
        isFirstWelcome: Boolean,
        removedContactIds: Set<String>,
        persistedAt: Long
    ): Result<Unit>

    suspend fun authorizeIncomingActivation(
        packet: GroupMemberActivatedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray,
        transportMode: String
    ): Result<Boolean>

    suspend fun applyIncomingActivation(
        packet: GroupMemberActivatedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray,
        transportMode: String,
        memberContactId: String?,
        receivedAtEpochMilliseconds: Long
    ): Result<Boolean>

    suspend fun authorizeIncomingGroupRemoval(
        packet: GroupMemberRemovedPacket,
        senderContactId: String,
        pendingOwnerSigningPublicKey: ByteArray?
    ): Result<Boolean>

    suspend fun completeIncomingGroupRemoval(packet: GroupMemberRemovedPacket): Result<Unit>

    suspend fun authorizeIncomingGroupDeletion(
        packet: GroupConversationDeletedPacket,
        ownerContactId: String,
        ownerSigningPublicKey: ByteArray
    ): Result<Unit>

    suspend fun completeIncomingGroupDeletion(
        packet: GroupConversationDeletedPacket,
        ownerContactId: String
    ): Result<Unit>

    suspend fun receiveReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun receiveMemberActivationAcknowledgement(
        packet: GroupMemberActivationAcknowledgementPacket,
        acknowledgingContactId: String,
        transportMode: String
    ): Result<Unit>

    suspend fun receiveLeaveRequest(
        memberContactId: String,
        packet: GroupLeaveRequestPacket,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult>

    suspend fun resolveMetadataMessageSender(
        groupId: String,
        epoch: Int,
        signingPublicKey: ByteArray
    ): Result<GroupMetadataMessageSender>

    suspend fun wasGroupDeleted(groupId: String): Result<Boolean>

    suspend fun inspectMessageAccess(groupId: String): Result<GroupMessageMembershipAccess>

    /** Resolves a pinned message sender against the current group epoch.
     * A missing remote member key returns null so Chats can use a known contact identity.
     */
    suspend fun resolvePinnedSenderSigningKey(
        groupId: String,
        isMine: Boolean,
        senderContactId: String?
    ): Result<ByteArray?>

    suspend fun getCurrentEpoch(groupId: String): Result<Int>

    /** Validate the ready acknowledgement against Membership's installed group key. */
    suspend fun verifyGroupKeyConfirmation(
        groupId: String,
        epoch: Int,
        confirmation: ByteArray
    ): Result<Unit>

    /** Read-only Membership authorization for outbound group metadata packets. */
    suspend fun authorizeMetadataSend(
        groupId: String,
        localSigningPublicKey: ByteArray,
        action: String
    ): Result<GroupMetadataSendContext>

    /** Returns false for a stale epoch; rejects future epochs or unauthorized signers. */
    suspend fun authorizeMetadataReceive(
        groupId: String,
        epoch: Int,
        contactId: String,
        adminSigningPublicKey: ByteArray,
        action: String
    ): Result<Boolean>

    fun observeAdministration(groupId: String): Flow<GroupAdministrationState>

    /** Membership-owned persistence snapshot; Chats may observe without reading foreign DAOs. */
    fun observeConversationMembership(groupId: String): Flow<GroupConversationMembershipSnapshot>

    fun observeVerificationSnapshot(groupId: String): Flow<MembershipVerificationSnapshot>

    suspend fun removeMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult>

    suspend fun promoteMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberPromotionResult>

    suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEnd>

    suspend fun getLeaveRequirement(groupId: String): Result<GroupLeaveRequirement>

    suspend fun leave(
        groupId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEnd>

    suspend fun delete(
        groupId: String,
        context: GroupMembershipContext
    ): Result<Long>
}
