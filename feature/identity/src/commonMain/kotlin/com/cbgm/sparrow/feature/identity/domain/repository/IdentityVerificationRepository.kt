package com.cbgm.sparrow.feature.identity.domain.repository

import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.ContactVerificationReceiptPacket

interface IdentityVerificationRepository {
    suspend fun verify(contactId: String): Result<Unit>

    suspend fun sendReceiptIfLocallyVerified(contactId: String): Result<Unit>

    suspend fun receiveReceipt(
        context: IncomingPacketContext,
        packet: ContactVerificationReceiptPacket
    ): Result<Unit>
}
