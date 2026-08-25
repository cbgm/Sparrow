package com.cbgm.sparrow.feature.contacts.data.repository

import com.cbgm.sparrow.core.protocol.handler.IncomingPacketContext
import com.cbgm.sparrow.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.sparrow.feature.contacts.data.datasource.ContactVerificationDataSource
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactVerificationRepository

class ContactVerificationRepositoryImpl(
    private val dataSource: ContactVerificationDataSource
) : ContactVerificationRepository {
    override suspend fun verify(contactId: String): Result<Unit> =
        dataSource.verify(contactId)

    override suspend fun sendReceiptIfLocallyVerified(contactId: String): Result<Unit> =
        dataSource.sendReceiptIfLocallyVerified(contactId)

    override suspend fun receiveReceipt(
        context: IncomingPacketContext,
        packet: ContactVerificationReceiptPacket
    ): Result<Unit> =
        dataSource.receiveReceipt(context, packet)
}
