package com.cbgm.sparrow.feature.invite.data.protocol.handler

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.invite.data.protocol.InvitationPacketProcessor
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationResponse
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleInvitationResponseUseCase

class InvitationDeclinedPacketHandler(
    private val handleInvitationResponse: HandleInvitationResponseUseCase,
    private val invitationPacketProcessor: InvitationPacketProcessor
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInviteDeclinedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> {
        val declinedPacket =
            packet as? ContactInviteDeclinedPacket
                ?: error("Incompatible invitation decline packet")

        return handleInvitationResponse(
            payloadType = InvitationPayloadType.DIRECT,
            invitationId = declinedPacket.invitationId,
            response = InvitationResponse.DECLINED,
            applyResponseEffects = {
                invitationPacketProcessor.receiveDeclined(
                    context = context,
                    packet = declinedPacket
                )
            }
        )
    }
}
