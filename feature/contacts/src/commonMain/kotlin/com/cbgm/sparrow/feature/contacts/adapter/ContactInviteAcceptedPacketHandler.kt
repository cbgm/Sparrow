package com.cbgm.sparrow.feature.contacts.adapter

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase

class ContactInviteAcceptedPacketHandler(
    private val handleInvitationResponse: HandleInvitationResponseUseCase,
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInviteAcceptedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> {
        val acceptedPacket =
            packet as? ContactInviteAcceptedPacket
                ?: error("Incompatible contact acceptance packet")

        return handleInvitationResponse(
            invitationId = acceptedPacket.invitationId,
            response = InvitationResponse.ACCEPTED,
            applyResponseEffects = {
                directIdentityExchangeRepository.receiveAccepted(
                    context = context,
                    packet = acceptedPacket
                )
            }
        )
    }
}
