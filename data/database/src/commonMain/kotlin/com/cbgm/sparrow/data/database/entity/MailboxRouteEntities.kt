package com.cbgm.sparrow.data.database.entity

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

/**
 * A local mailbox replaced by a renewal. It stays readable until it expires, so envelopes still in it and those
 * the contact sends before it learned the new route are not lost; it is revoked after its expiry.
 */
@Entity(
    tableName = "retiring_mailbox_credentials",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["contactId"])]
)
data class RetiringMailboxCredentialEntity(
    @PrimaryKey val routeId: String,
    val contactId: String,
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
