package com.cbgm.sparrow.feature.conversationorchestration.data.group.invitation

import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.feature.invite.data.protocol.GroupInvitationPacketProcessor
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse

internal class GroupInvitationPacketProcessorImpl(
    private val inviteProcessor: GroupInviteIncomingProcessorImpl,
    private val inviteReceivedProcessor: GroupInviteReceivedIncomingProcessorImpl,
    private val inviteDeclinedProcessor: GroupInviteDeclinedIncomingProcessorImpl,
    private val joinRequestProcessor: GroupJoinRequestIncomingProcessorImpl
) : GroupInvitationPacketProcessor {
    override suspend fun receiveInvite(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long,
        shouldStage: Boolean
    ): Result<Unit> =
        inviteProcessor.process(
            ownerContactId = ownerContactId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds,
            shouldStage = shouldStage
        )

    override suspend fun receiveInviteReceived(
        memberContactId: String,
        packet: GroupInviteReceivedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        inviteReceivedProcessor.process(
            memberContactId = memberContactId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )

    override suspend fun receiveDeclined(
        memberContactId: String,
        packet: GroupInviteDeclinedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<InvitationResponse?> =
        inviteDeclinedProcessor.process(
            memberContactId = memberContactId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )

    override suspend fun receiveJoinRequest(
        memberContactId: String,
        packet: GroupJoinRequestPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<InvitationResponse?> =
        joinRequestProcessor.process(
            memberContactId = memberContactId,
            packet = packet,
            receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
        )
}
