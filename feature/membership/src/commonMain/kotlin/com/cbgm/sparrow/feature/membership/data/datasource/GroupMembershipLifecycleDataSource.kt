package com.cbgm.sparrow.feature.membership.data.datasource

import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.feature.membership.data.model.GroupLocalMembershipEndDto
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberPromotionResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalResult
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext

/**
 * Transitional facade for established group-membership lifecycle operations.
 *
 * Each branch is resolved only when that operation is actually used. This prevents the packet
 * routing graph from eagerly constructing unrelated administration/deletion dependencies while
 * those remaining cross-feature seams are being removed.
 */
class GroupMembershipLifecycleDataSource internal constructor(
    private val activationProvider: () -> GroupMembershipActivationDataSource,
    private val administrationProvider: () -> GroupMembershipAdministrationDataSource,
    private val deletionProvider: () -> GroupMembershipDeletionDataSource
) {
    private val activation: GroupMembershipActivationDataSource
        get() = activationProvider()

    private val administration: GroupMembershipAdministrationDataSource
        get() = administrationProvider()

    private val deletion: GroupMembershipDeletionDataSource
        get() = deletionProvider()

    suspend fun removeMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> = administration.removeMember(groupId, contactId, context)

    suspend fun promoteMember(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupMemberPromotionResult> = administration.promoteMember(groupId, contactId, context)

    internal suspend fun transferAdminAndLeave(
        groupId: String,
        contactId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEndDto> = administration.transferAdminAndLeave(groupId, contactId, context)

    internal suspend fun leaveGroup(
        groupId: String,
        context: GroupMembershipContext
    ): Result<GroupLocalMembershipEndDto> = administration.leaveGroup(groupId, context)

    suspend fun deleteGroupConversation(
        groupId: String,
        context: GroupMembershipContext
    ): Result<Long> = deletion.deleteGroupConversation(groupId, context)

    suspend fun receiveLeaveRequest(
        memberContactId: String,
        packet: GroupLeaveRequestPacket,
        context: GroupMembershipContext
    ): Result<GroupMemberRemovalResult> = administration.receiveLeaveRequest(memberContactId, packet, context)

    suspend fun receiveReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        activation.receiveReadyAcknowledgement(
            memberContactId = memberContactId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )

    suspend fun receiveMemberActivationAcknowledgement(
        packet: GroupMemberActivationAcknowledgementPacket,
        acknowledgingContactId: String,
        transportMode: String
    ): Result<Unit> =
        activation.receiveMemberActivationAcknowledgement(
            packet = packet,
            acknowledgingContactId = acknowledgingContactId,
            transportMode = transportMode
        )
}
