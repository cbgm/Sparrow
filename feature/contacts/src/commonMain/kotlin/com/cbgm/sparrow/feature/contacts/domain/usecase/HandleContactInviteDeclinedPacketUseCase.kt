package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository

class HandleContactInviteDeclinedPacketUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactInviteDeclinedPacket
    ): Result<Unit> =
        identityInvitationRepository.receiveDeclined(context, packet)
}
