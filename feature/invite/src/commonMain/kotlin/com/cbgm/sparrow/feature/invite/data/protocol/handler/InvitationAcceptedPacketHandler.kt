package com.cbgm.sparrow.feature.invite.data.protocol.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.invite.data.protocol.InvitationPacketProcessor
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase

class InvitationAcceptedPacketHandler(
    private val handleInvitationResponse: HandleInvitationResponseUseCase,
    private val invitationPacketProcessor: InvitationPacketProcessor
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInviteAcceptedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> {
        val acceptedPacket =
            packet as? ContactInviteAcceptedPacket
                ?: error("Incompatible invitation acceptance packet")

        return handleInvitationResponse(
            invitationId = acceptedPacket.invitationId,
            response = InvitationResponse.ACCEPTED,
            applyResponseEffects = {
                invitationPacketProcessor.receiveAccepted(
                    context = context,
                    packet = acceptedPacket
                )
            }
        )
    }
}
