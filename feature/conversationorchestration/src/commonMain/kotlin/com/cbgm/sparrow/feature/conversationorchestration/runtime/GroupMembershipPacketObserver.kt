package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.protocol.packet.GroupConversationDeletedPacket
import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.sparrow.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.SparrowPacket

internal class GroupMembershipPacketObserver(
    private val flowHandler: ConversationFlowHandler
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean =
        packet is GroupCreatedPacket ||
            packet is GroupMemberRemovedPacket ||
            packet is GroupMemberActivatedPacket ||
            packet is GroupConversationDeletedPacket ||
            packet is GroupInvitePacket ||
            packet is GroupInviteReceivedPacket ||
            packet is GroupInviteDeclinedPacket ||
            packet is GroupJoinRequestPacket ||
            packet is GroupReadyAcknowledgementPacket ||
            packet is GroupMemberActivationAcknowledgementPacket ||
            packet is GroupLeaveRequestPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        flowHandler.onMembershipPacket(context, packet)
}
