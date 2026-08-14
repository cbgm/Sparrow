package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistence representation of a person known to Sparrow.
 *
 * Phone numbers and Sparrow keys are stored in separate tables.
 */
@Entity(
    tableName = "contacts",
    indices = [
        Index(
            value = ["deviceContactId"],
            unique = false
        ),

        Index(
            value = ["preferredPhoneNumberId"],
            unique = false
        )
    ]
)
data class ContactEntity(
    @PrimaryKey
    val id: String,
    val displayName: String?,
    val deviceContactId: String?,
    val deviceContactLinkStatus: String,
    /**
     * ID of the phone number currently preferred for actions such
     * as SMS.
     *
     * Null means that no preferred number has been selected.
     *
     * This is deliberately not a Room foreign key because phone
     * numbers already reference contacts, and a reverse foreign key
     * would create a circular database relationship.
     */
    val preferredPhoneNumberId: String?,
    val createdAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long
)
