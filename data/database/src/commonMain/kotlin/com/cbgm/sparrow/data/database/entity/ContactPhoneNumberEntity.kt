package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contact_phone_numbers",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contactId"]),
        Index(value = ["normalizedValue"])
    ]
)
data class ContactPhoneNumberEntity(
    @PrimaryKey
    val id: String,
    val contactId: String,
    val value: String,
    /**
     * Stable normalized representation used for matching and routing.
     */
    val normalizedValue: String,
    val type: String,
    val label: String?,
    val updatedAtEpochMilliseconds: Long
)
