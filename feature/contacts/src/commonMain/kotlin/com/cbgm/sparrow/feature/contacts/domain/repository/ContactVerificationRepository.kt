package com.cbgm.sparrow.feature.contacts.domain.repository

interface ContactVerificationRepository {
    suspend fun verify(contactId: String): Result<Unit>

    suspend fun sendReceiptIfLocallyVerified(contactId: String): Result<Unit>
}
