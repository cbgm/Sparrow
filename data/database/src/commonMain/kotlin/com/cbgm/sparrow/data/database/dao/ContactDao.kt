package com.cbgm.sparrow.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.sparrow.data.database.model.ContactWithPhoneNumbersDto
import com.cbgm.sparrow.data.database.model.ContactWithPublicIdentityDto
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Upsert
    suspend fun upsertContact(contact: ContactEntity)

    @Query(
        """
        UPDATE contacts
        SET displayName = :phoneNumber,
            updatedAtEpochMilliseconds = :updatedAtEpochMilliseconds
        WHERE id = :contactId
          AND (displayName IS NULL OR TRIM(displayName) = '')
        """
    )
    suspend fun usePhoneNumberAsDisplayNameWhenMissing(
        contactId: String,
        phoneNumber: String,
        updatedAtEpochMilliseconds: Long
    )

    @Query(
        """
    SELECT contacts.*
    FROM contacts
    INNER JOIN contact_phone_numbers
        ON contact_phone_numbers.contactId = contacts.id
    WHERE contact_phone_numbers.normalizedValue =
        :normalizedPhoneNumber
    LIMIT 1
    """
    )
    suspend fun findContactEntityByNormalizedPhoneNumber(normalizedPhoneNumber: String): ContactEntity?

    @Transaction
    @Query(
        """
        SELECT *
        FROM contacts
        WHERE id = :contactId
        LIMIT 1
        """
    )
    suspend fun findById(contactId: String): ContactWithPublicIdentityDto?

    @Transaction
    @Query(
        """
        SELECT *
        FROM contacts
        ORDER BY
            CASE
                WHEN displayName IS NULL THEN 1
                ELSE 0
            END,
            displayName COLLATE NOCASE,
            createdAtEpochMilliseconds
        """
    )
    fun observeAll(): Flow<List<ContactWithPublicIdentityDto>>

    /** Contact lookup for device-address-book reconciliation; no Identity relation is loaded. */
    @Transaction
    @Query("SELECT * FROM contacts WHERE deviceContactId = :deviceContactId LIMIT 1")
    suspend fun findContactWithPhoneNumbersByDeviceContactId(
        deviceContactId: String
    ): ContactWithPhoneNumbersDto?

    /** Contacts-only reads for incoming Identity peer resolution. */
    @Transaction
    @Query("SELECT * FROM contacts WHERE id = :contactId LIMIT 1")
    suspend fun findContactWithPhoneNumbers(contactId: String): ContactWithPhoneNumbersDto?

    @Transaction
    @Query(
        """SELECT contacts.* FROM contacts
        INNER JOIN contact_phone_numbers ON contact_phone_numbers.contactId = contacts.id
        WHERE contact_phone_numbers.normalizedValue = :normalizedPhoneNumber LIMIT 1"""
    )
    suspend fun findContactWithPhoneNumbersByNormalizedPhoneNumber(
        normalizedPhoneNumber: String
    ): ContactWithPhoneNumbersDto?

    @Transaction
    @Query(
        """SELECT * FROM contacts ORDER BY
        CASE WHEN displayName IS NULL THEN 1 ELSE 0 END,
        displayName COLLATE NOCASE, createdAtEpochMilliseconds"""
    )
    fun observeContactsWithPhoneNumbers(): Flow<List<ContactWithPhoneNumbersDto>>

    @Upsert
    suspend fun upsertPhoneNumbers(phoneNumbers: List<ContactPhoneNumberEntity>)

    @Query(
        """
        DELETE FROM contact_phone_numbers
        WHERE contactId = :contactId
        """
    )
    suspend fun deletePhoneNumbersForContact(contactId: String)

    @Query(
        """
        DELETE FROM contacts
        WHERE id = :contactId
        """
    )
    suspend fun deleteById(contactId: String)

    @Query(
        """
    SELECT *
    FROM contact_public_identities
    WHERE contactId = :contactId
    LIMIT 1
    """
    )
    suspend fun findPublicIdentityByContactId(contactId: String): ContactPublicIdentityEntity?
}
