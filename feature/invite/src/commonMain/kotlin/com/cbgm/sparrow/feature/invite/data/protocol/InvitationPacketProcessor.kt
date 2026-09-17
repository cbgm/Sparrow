package com.cbgm.sparrow.feature.invite.data.protocol

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.feature.invite.domain.model.InvitationLifecycleRecord

interface InvitationPacketProcessor {
    suspend fun receiveInvite(
        context: IncomingPacketContext,
        packet: ContactInvitePacket,
        receptionEnabled: Boolean,
        blockedPeerIds: Set<String>,
        blockUnknownPeers: Boolean
    ): Result<InvitationLifecycleRecord?>

    suspend fun receiveAccepted(
        context: IncomingPacketContext,
        packet: ContactInviteAcceptedPacket
    ): Result<Unit>

    suspend fun receiveDeclined(
        context: IncomingPacketContext,
        packet: ContactInviteDeclinedPacket
    ): Result<Unit>
}
