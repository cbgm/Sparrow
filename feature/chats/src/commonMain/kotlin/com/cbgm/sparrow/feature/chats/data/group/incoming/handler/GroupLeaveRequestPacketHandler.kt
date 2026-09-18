package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupLeaveRequestPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.conversationorchestration.domain.port.ConversationPort
import com.cbgm.sparrow.feature.membership.data.datasource.GroupMembershipLifecycleDataSource
import com.cbgm.sparrow.feature.membership.domain.model.GroupMemberRemovalReason

class GroupLeaveRequestPacketHandler(
    private val membershipCoordinator: GroupMembershipLifecycleDataSource,
    private val conversationPort: ConversationPort
) : GroupPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupLeaveRequestPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        runCatching {
            val leaveRequest = packet as GroupLeaveRequestPacket
            val groupContext = conversationPort.getGroupMembershipContext(leaveRequest.groupId).getOrThrow()
            val result =
                membershipCoordinator
                    .receiveLeaveRequest(
                        memberContactId = context.contactId,
                        packet = leaveRequest,
                        context = groupContext
                    ).getOrThrow()
            conversationPort
                .removeGroupParticipant(
                    groupId = result.groupId,
                    peerId = result.contactId,
                    epoch = result.epoch,
                    eventId = result.eventId,
                    updatedAtEpochMilliseconds = result.updatedAtEpochMilliseconds,
                    memberLeft = result.reason == GroupMemberRemovalReason.LEFT
                ).getOrThrow()
        }
}
