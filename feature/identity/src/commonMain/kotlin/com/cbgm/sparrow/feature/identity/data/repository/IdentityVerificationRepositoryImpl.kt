package com.cbgm.sparrow.feature.identity.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.identity.data.datasource.IdentityVerificationDataSource
import com.cbgm.sparrow.feature.identity.domain.repository.IdentityVerificationRepository
import com.cbgm.sparrow.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.protocol.packet.ContactVerificationReceiptPacket

internal class IdentityVerificationRepositoryImpl(
    private val dataSource: IdentityVerificationDataSource
) : IdentityVerificationRepository {
    override suspend fun verify(contactId: String): Result<Unit> =
        safeSuspendCall { dataSource.verify(contactId) }

    override suspend fun sendReceiptIfLocallyVerified(contactId: String): Result<Unit> =
        safeSuspendCall { dataSource.sendReceiptIfLocallyVerified(contactId) }

    override suspend fun receiveReceipt(
        context: IncomingPacketContext,
        packet: ContactVerificationReceiptPacket
    ): Result<Unit> =
        safeSuspendCall { dataSource.receiveReceipt(context, packet) }
}
