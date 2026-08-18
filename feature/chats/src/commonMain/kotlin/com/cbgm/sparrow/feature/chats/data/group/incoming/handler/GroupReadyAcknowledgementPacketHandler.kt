package com.cbgm.sparrow.feature.chats.data.group.incoming.handler

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.chats.data.group.avatar.GroupAvatarBroadcaster
import com.cbgm.sparrow.feature.chats.data.group.membership.GroupMembershipCoordinator

class GroupReadyAcknowledgementPacketHandler internal constructor(
    private val membershipCoordinator: GroupMembershipCoordinator,
    private val groupAvatarBroadcaster: GroupAvatarBroadcaster
) : GroupPacketHandler {
    private val logger = SparrowLog.withTag("GroupReadyAcknowledgementPacketHandler")

    override fun canHandle(packet: SparrowPacket): Boolean = packet is GroupReadyAcknowledgementPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> =
        membershipCoordinator
            .receiveReadyAcknowledgement(
                memberContactId = context.contactId,
                packet = packet as GroupReadyAcknowledgementPacket,
                receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
            ).onSuccess {
                groupAvatarBroadcaster
                    .sendCurrentTo(
                        groupId = context.conversationId,
                        contactId = context.contactId
                    ).onFailure { error ->
                        logger.warn(error) {
                            "Could not queue current group avatar for ${context.contactId}"
                        }
                    }
            }
}
