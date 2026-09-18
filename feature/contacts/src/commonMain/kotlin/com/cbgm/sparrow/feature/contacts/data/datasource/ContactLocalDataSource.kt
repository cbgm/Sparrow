package com.cbgm.sparrow.feature.contacts.data.datasource

import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.sparrow.data.database.model.ContactWithPublicIdentityDto
import kotlinx.coroutines.flow.Flow

class ContactLocalDataSource(
    private val contactDao: ContactDao
) {
    suspend fun findById(contactId: String): ContactWithPublicIdentityDto? =
        contactDao.findById(contactId)

    suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): ContactWithPublicIdentityDto? =
        contactDao.findBySigningPublicKey(signingPublicKey)

    suspend fun findByNormalizedPhoneNumber(phoneNumber: String): ContactWithPublicIdentityDto? =
        contactDao.findByNormalizedPhoneNumber(phoneNumber)

    suspend fun findByDeviceContactId(deviceContactId: String): ContactWithPublicIdentityDto? =
        contactDao.findByDeviceContactId(deviceContactId)

    suspend fun findPublicIdentityByContactId(contactId: String): ContactPublicIdentityEntity? =
        contactDao.findPublicIdentityByContactId(contactId)

    fun observeAll(): Flow<List<ContactWithPublicIdentityDto>> =
        contactDao.observeAll()

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

    suspend fun updateVerificationStatusIfKeysMatch(
        contactId: String,
        expectedEncryptionPublicKey: ByteArray,
        expectedSigningPublicKey: ByteArray,
        verificationStatus: String,
        updatedAtEpochMilliseconds: Long
    ) = contactDao.updateVerificationStatusIfKeysMatch(
        contactId = contactId,
        expectedEncryptionPublicKey = expectedEncryptionPublicKey,
        expectedSigningPublicKey = expectedSigningPublicKey,
        verificationStatus = verificationStatus,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )

    suspend fun updateKeyExchangeStatusIfKeysMatch(
        contactId: String,
        expectedEncryptionPublicKey: ByteArray,
        expectedSigningPublicKey: ByteArray,
        keyExchangeStatus: String,
        updatedAtEpochMilliseconds: Long
    ) = contactDao.updateKeyExchangeStatusIfKeysMatch(
        contactId = contactId,
        expectedEncryptionPublicKey = expectedEncryptionPublicKey,
        expectedSigningPublicKey = expectedSigningPublicKey,
        keyExchangeStatus = keyExchangeStatus,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
    )

    suspend fun updateKeyExchangeStatus(
        contactId: String,
        status: String,
        updatedAt: Long
    ) = contactDao.updateKeyExchangeStatus(
        contactId = contactId,
        status = status,
        updatedAt = updatedAt
    )

    suspend fun updateVerificationStatus(
        contactId: String,
        status: String,
        updatedAt: Long
    ) = contactDao.updateVerificationStatus(
        contactId = contactId,
        status = status,
        updatedAt = updatedAt
    )

    suspend fun clearVerifiedByContact(
        contactId: String,
        updatedAt: Long
    ) = contactDao.clearVerifiedByContact(
        contactId = contactId,
        updatedAt = updatedAt
    )
}
