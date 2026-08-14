package com.cbgm.securechat.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_mailbox_credentials",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["contactId"], unique = true)]
)
data class LocalMailboxCredentialEntity(
    @PrimaryKey val contactId: String,
    val routeId: String,
    val nodeId: String,
    val nodeEndpoint: String,
    val mailboxId: String,
    val sendCapability: String,
    val accessEndpoint: String,
    val retrievalCapability: String,
    val sequence: Long,
    val expiresAtEpochMilliseconds: Long,
    val identitySignature: ByteArray,
    val revocationPending: Boolean = false
)

@Entity(
    tableName = "remote_mailbox_routes",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["contactId"], unique = true)]
)
data class RemoteMailboxRouteEntity(
    @PrimaryKey val contactId: String,
    val routeId: String,
    val nodeId: String,
    val nodeEndpoint: String,
    val mailboxId: String,
    val sendCapability: String,
    val sequence: Long,
    val expiresAtEpochMilliseconds: Long,
    val identitySignature: ByteArray
)
