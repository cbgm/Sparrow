package com.cbgm.sparrow.feature.contacts.domain.repository

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    suspend fun importDeviceContact(request: ImportDeviceContactRequest): Result<Contact>

    suspend fun importContact(request: ImportContactRequest): Result<Contact>

    suspend fun getContact(contactId: String): Result<Contact?>

    suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): Result<Contact?>

    suspend fun findOrCreateByPhoneNumber(phoneNumber: String): Result<Contact>

    fun observeContacts(): Flow<List<Contact>>

    suspend fun updateContactDetails(
        contactId: String,
        displayName: String?,
        phoneNumber: String?
    ): Result<Contact>

    suspend fun markVerified(contactId: String): Result<Contact>

    /**
     * Marks that both parties possess each other's current keys.
     *
     * Do not call this merely after importing their identity.
     * It should only be called after an authenticated acknowledgement
     * from the remote device.
     */
    suspend fun markKeyExchangeMutual(contactId: String): Result<Contact>

    /**
     * Resets the exchange to one-way and removes verification.
     */
    suspend fun resetKeyExchange(contactId: String): Result<Contact>

    suspend fun updateDeviceContactLinkStatus(
        deviceContactId: String,
        status: DeviceContactLinkStatus
    ): Result<Contact?>
}
