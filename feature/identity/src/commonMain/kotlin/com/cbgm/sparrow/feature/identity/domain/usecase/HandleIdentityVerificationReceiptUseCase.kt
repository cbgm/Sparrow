package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityVerificationRepository

class HandleIdentityVerificationReceiptUseCase(
    private val contactVerificationRepository: IdentityVerificationRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactVerificationReceiptPacket
    ): Result<Unit> =
        contactVerificationRepository.receiveReceipt(context, packet)
}
