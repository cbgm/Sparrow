package com.cbgm.sparrow.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Append-only until acknowledged; no foreign key to a mutable or garbage-collected outbox row. */
@Entity(
    tableName = "protocol_outbox_failure_events",
    indices = [Index(value = ["packetId", "attemptCount"])]
)
data class ProtocolOutboxFailureEventEntity(
    @PrimaryKey val eventId: String,
    val packetId: String,
    val encodedPacket: ByteArray,
    val attemptCount: Int,
    val errorMessage: String,
    val occurredAtEpochMilliseconds: Long
)
