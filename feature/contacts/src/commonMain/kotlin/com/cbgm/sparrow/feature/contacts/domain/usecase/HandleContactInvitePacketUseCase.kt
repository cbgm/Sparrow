package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.IdentityInvitationRepository

class HandleContactInvitePacketUseCase(
    private val identityInvitationRepository: IdentityInvitationRepository,
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactInvitePacket
    ): Result<Unit> =
        identityInvitationRepository.receiveInvite(
            context = context,
            packet = packet,
            setupMode = modeRepository.getMode(),
            blockedContactIds = contactBlocklistRepository.getBlockedContactIds(),
            blockUnknownContactInvites = contactBlocklistRepository.getBlockUnknownContactInvites()
        )
}
