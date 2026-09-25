package com.cbgm.sparrow.feature.contacts.domain.repository

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.ImportDeviceContactRequest
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    suspend fun importDeviceContact(request: ImportDeviceContactRequest): Result<Contact>

    /** Persists contact details only; the calling use case must then persist the keys with Identity. */
    suspend fun upsertImportedContact(request: ImportContactRequest): Result<Contact>

    suspend fun getContact(contactId: String): Result<Contact?>

    /** A validated incoming sender phone number is used only if the contact has no display name. */
    suspend fun usePhoneNumberAsDisplayNameWhenMissing(
        contactId: String,
        phoneNumber: String,
        updatedAtEpochMilliseconds: Long
    ): Result<Unit>

    suspend fun findOrCreateByPhoneNumber(phoneNumber: String): Result<Contact>

    /** The packet was already authenticated; update the sender or create an address-book contact. */
    suspend fun resolveAuthenticatedPeerContact(senderContactId: String?, phoneNumber: String?): Result<String>

    fun observeContacts(): Flow<List<Contact>>

    suspend fun updateContactDetails(
        contactId: String,
        displayName: String?,
        phoneNumber: String?
    ): Result<Contact>

    suspend fun updateDeviceContactLinkStatus(
        deviceContactId: String,
        status: DeviceContactLinkStatus
    ): Result<Contact?>
}
