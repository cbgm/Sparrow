package com.cbgm.sparrow.feature.invite.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.security.ContactBlocklistRepository
import com.cbgm.sparrow.core.security.DirectIdentitySetupModeRepository
import com.cbgm.sparrow.feature.invite.domain.repository.DirectIdentityExchangeRepository

class HandleContactInvitePacketUseCase(
    private val directIdentityExchangeRepository: DirectIdentityExchangeRepository,
    private val modeRepository: DirectIdentitySetupModeRepository,
    private val contactBlocklistRepository: ContactBlocklistRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactInvitePacket
    ): Result<Unit> =
        directIdentityExchangeRepository.receiveInvite(
            context = context,
            packet = packet,
            setupMode = modeRepository.getMode(),
            blockedContactIds = contactBlocklistRepository.getBlockedContactIds(),
            blockUnknownContactInvites = contactBlocklistRepository.getBlockUnknownContactInvites()
        )
}
