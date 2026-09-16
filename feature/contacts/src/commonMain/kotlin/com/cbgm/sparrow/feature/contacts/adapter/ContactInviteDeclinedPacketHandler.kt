package com.cbgm.sparrow.feature.contacts.adapter

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase

class ContactInviteDeclinedPacketHandler(
    private val handleInvitationResponse: HandleInvitationResponseUseCase,
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInviteDeclinedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> {
        val declinedPacket =
            packet as? ContactInviteDeclinedPacket
                ?: error("Incompatible contact decline packet")

        return handleInvitationResponse(
            invitationId = declinedPacket.invitationId,
            response = InvitationResponse.DECLINED,
            applyResponseEffects = {
                directIdentityExchangeRepository.receiveDeclined(
                    context = context,
                    packet = declinedPacket
                )
            }
        )
    }
}
