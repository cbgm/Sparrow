package com.cbgm.sparrow.feature.conversationorchestration.runtime

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.conversationorchestration.domain.workflow.ConversationFlowHandler

internal class GroupMembershipPacketObserver(
    private val flowHandler: ConversationFlowHandler
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean =
        packet is GroupInvitePacket ||
            packet is GroupInviteReceivedPacket ||
            packet is GroupInviteDeclinedPacket ||
            packet is GroupJoinRequestPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        flowHandler.onMembershipPacket(context, packet)
}
