package com.cbgm.sparrow.feature.invite.data.protocol.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.invite.data.protocol.InvitationPacketProcessor
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleIncomingInvitationUseCase
import com.cbgm.sparrow.feature.invite.domain.usecase.RecordPendingInvitationUseCase

class IncomingInvitationPacketHandler(
    private val handleIncomingInvitation: HandleIncomingInvitationUseCase,
    private val recordPendingInvitation: RecordPendingInvitationUseCase,
    private val invitationPacketProcessor: InvitationPacketProcessor
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInvitePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> {
        val invitePacket =
            packet as? ContactInvitePacket
                ?: error("Incompatible invitation packet")

        return handleIncomingInvitation incoming@{ receptionPolicy ->
            val receiveResult =
                invitationPacketProcessor.receiveInvite(
                    context = context,
                    packet = invitePacket,
                    receptionEnabled = receptionPolicy.enabled,
                    blockedPeerIds = receptionPolicy.blockedPeerIds,
                    blockUnknownPeers = receptionPolicy.blockUnknownPeers
                )

            val record = receiveResult.getOrElse { error ->
                return@incoming Result.failure(error)
            }

            if (record == null) {
                Result.success(Unit)
            } else {
                recordPendingInvitation(record)
            }
        }
    }
}
