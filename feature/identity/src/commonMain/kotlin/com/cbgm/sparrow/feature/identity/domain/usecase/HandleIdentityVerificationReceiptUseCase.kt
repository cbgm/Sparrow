package com.cbgm.sparrow.feature.identity.domain.usecase

import com.cbgm.sparrow.feature.identity.domain.repository.IdentityVerificationRepository
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.ContactVerificationReceiptPacket

class HandleIdentityVerificationReceiptUseCase(
    private val contactVerificationRepository: IdentityVerificationRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactVerificationReceiptPacket
    ): Result<Unit> =
        contactVerificationRepository.receiveReceipt(context, packet)
}
