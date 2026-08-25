package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactReadyPacket
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository

class HandleContactReadyPacketUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactReadyPacket
    ): Result<Unit> =
        identityInvitationRepository.receiveReady(context, packet)
}
