package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "contact_profile_pictures",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ContactProfilePictureEntity(
    @PrimaryKey
    val contactId: String,
    val hasPicture: Boolean,
    val changedAtEpochMilliseconds: Long
)
