package com.cbgm.sparrow.data.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity

/** Contacts-owned record. Never loads Identity's keys or trust state. */
data class ContactWithPhoneNumbersDto(
    @Embedded val contact: ContactEntity,
    @Relation(parentColumn = "id", entityColumn = "contactId")
    val phoneNumbers: List<ContactPhoneNumberEntity>
)
