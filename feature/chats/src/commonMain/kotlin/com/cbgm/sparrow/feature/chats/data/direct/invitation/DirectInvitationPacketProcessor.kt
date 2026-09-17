package com.cbgm.sparrow.feature.chats.data.direct.invitation

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.feature.invite.data.protocol.InvitationPacketProcessor

internal class DirectInvitationPacketProcessor(
    private val coordinator: DirectIdentityExchangeCoordinator
) : InvitationPacketProcessor {
    override suspend fun receiveInvite(
        context: IncomingPacketContext,
        packet: ContactInvitePacket,
        receptionEnabled: Boolean,
        blockedPeerIds: Set<String>,
        blockUnknownPeers: Boolean
    ): Result<Unit> =
        coordinator.receiveInvite(
            context = context,
            packet = packet,
            receptionEnabled = receptionEnabled,
            blockedPeerIds = blockedPeerIds,
            blockUnknownPeers = blockUnknownPeers
        )

    override suspend fun receiveAccepted(
        context: IncomingPacketContext,
        packet: ContactInviteAcceptedPacket
    ): Result<Unit> = coordinator.receiveAccepted(context, packet)

    override suspend fun receiveDeclined(
        context: IncomingPacketContext,
        packet: ContactInviteDeclinedPacket
    ): Result<Unit> = coordinator.receiveDeclined(context, packet)
}
