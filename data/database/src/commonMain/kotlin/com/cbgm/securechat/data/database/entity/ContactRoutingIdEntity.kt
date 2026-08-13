package com.cbgm.securechat.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "contact_routing_ids",
    primaryKeys = ["contactId"],
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["contactId"], unique = true),
        Index(value = ["routingId"], unique = true)
    ]
)
data class ContactRoutingIdEntity(
    val contactId: String,
    val routingId: String
)
