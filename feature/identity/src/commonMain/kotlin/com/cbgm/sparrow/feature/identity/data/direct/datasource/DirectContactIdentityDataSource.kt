package com.cbgm.sparrow.feature.identity.data.direct.datasource

import com.cbgm.sparrow.data.database.dao.ContactDao
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.sparrow.data.database.model.ContactWithPublicIdentityDto
import kotlinx.coroutines.flow.Flow

internal class DirectContactIdentityDataSource(
    private val contactDao: ContactDao
) {
    suspend fun findById(contactId: String): ContactWithPublicIdentityDto? =
        contactDao.findById(contactId)

    suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): ContactWithPublicIdentityDto? =
        contactDao.findBySigningPublicKey(signingPublicKey)

    suspend fun findByNormalizedPhoneNumber(phoneNumber: String): ContactWithPublicIdentityDto? =
        contactDao.findByNormalizedPhoneNumber(phoneNumber)

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

    suspend fun upsertContact(contact: ContactEntity) = contactDao.upsertContact(contact)

    suspend fun upsertPhoneNumbers(phoneNumbers: List<ContactPhoneNumberEntity>) =
        contactDao.upsertPhoneNumbers(phoneNumbers)

    suspend fun deleteById(contactId: String) = contactDao.deleteById(contactId)
}
