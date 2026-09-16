package com.cbgm.sparrow.feature.contacts.adapter

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.SparrowPacket
import com.cbgm.sparrow.feature.identity.domain.repository.DirectIdentityExchangeRepository
import com.cbgm.sparrow.feature.invite.domain.usecase.HandleIncomingInvitationUseCase

class ContactInvitePacketHandler(
    private val handleIncomingInvitation: HandleIncomingInvitationUseCase,
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SparrowPacket): Boolean = packet is ContactInvitePacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SparrowPacket
    ): Result<Unit> {
        val invitePacket =
            packet as? ContactInvitePacket
                ?: error("Incompatible contact invite packet")

        return handleIncomingInvitation { receptionPolicy ->
            directIdentityExchangeRepository.receiveInvite(
                context = context,
                packet = invitePacket,
                receptionEnabled = receptionPolicy.enabled,
                blockedPeerIds = receptionPolicy.blockedPeerIds,
                blockUnknownPeers = receptionPolicy.blockUnknownPeers
            )
        }
    }
}
