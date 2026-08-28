package com.cbgm.sparrow.data.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.cbgm.sparrow.data.database.entity.ContactEntity
import com.cbgm.sparrow.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.sparrow.data.database.entity.ContactPublicIdentityEntity

data class ContactWithPublicIdentityDto(
    @Embedded
    val contact: ContactEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "contactId"
    )
    val publicIdentity: ContactPublicIdentityEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "contactId"
    )
    val phoneNumbers: List<ContactPhoneNumberEntity>
)
