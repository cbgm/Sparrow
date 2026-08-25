package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactVerificationRepository

class HandleContactVerificationReceiptPacketUseCase(
    private val contactVerificationRepository: ContactVerificationRepository
) {
    suspend operator fun invoke(
        context: IncomingPacketContext,
        packet: ContactVerificationReceiptPacket
    ): Result<Unit> =
        contactVerificationRepository.receiveReceipt(context, packet)
}
