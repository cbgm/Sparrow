package com.cbgm.sparrow.feature.contacts.data.datasource

import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.model.ContactWithPhoneNumbersDto
import kotlinx.coroutines.flow.Flow

class ContactLocalDataSource(
    private val contactDao: ContactDao
) {
    suspend fun findContactOnlyById(contactId: String): ContactWithPhoneNumbersDto? =
        contactDao.findContactWithPhoneNumbers(contactId)

    suspend fun findContactOnlyByNormalizedPhoneNumber(phoneNumber: String): ContactWithPhoneNumbersDto? =
        contactDao.findContactWithPhoneNumbersByNormalizedPhoneNumber(phoneNumber)

    fun observeContactsOnly(): Flow<List<ContactWithPhoneNumbersDto>> =
        contactDao.observeContactsWithPhoneNumbers()

    suspend fun findContactOnlyByDeviceContactId(deviceContactId: String): ContactWithPhoneNumbersDto? =
        contactDao.findContactWithPhoneNumbersByDeviceContactId(deviceContactId)

    suspend fun usePhoneNumberAsDisplayNameWhenMissing(
        contactId: String,
        phoneNumber: String,
        updatedAtEpochMilliseconds: Long
    ) = contactDao.usePhoneNumberAsDisplayNameWhenMissing(
        contactId = contactId,
        phoneNumber = phoneNumber,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )

    suspend fun upsertContact(contact: ContactEntity) =
        contactDao.upsertContact(contact)

    suspend fun upsertPhoneNumbers(phoneNumbers: List<ContactPhoneNumberEntity>) =
        contactDao.upsertPhoneNumbers(phoneNumbers)

    suspend fun deletePhoneNumbersForContact(contactId: String) =
        contactDao.deletePhoneNumbersForContact(contactId)

    suspend fun deleteById(contactId: String) =
        contactDao.deleteById(contactId)
}
