package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An incoming envelope that arrived before what it needs, for example a group message before the group's
 * welcome. It was acknowledged, so this row is its only copy until it is replayed.
 */
@Entity(
    tableName = "parked_envelopes",
    indices = [
        Index(value = ["envelopeId"], unique = true),
        Index("waitingKey"),
        Index("parkedAtEpochMilliseconds")
    ]
)
data class ParkedEnvelopeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val envelopeId: String,
    val senderRoutingId: String,
    val encodedTransportPayload: String,
    val waitingKey: String,
    val parkedAtEpochMilliseconds: Long
)
