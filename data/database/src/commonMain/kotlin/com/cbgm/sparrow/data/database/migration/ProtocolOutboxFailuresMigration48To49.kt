package com.cbgm.sparrow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Add only an independent attempt journal; preserve all existing outbox and invitation rows. */
object ProtocolOutboxFailuresMigration48To49 : Migration(48, 49) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS `protocol_outbox_failure_events` (
                `eventId` TEXT NOT NULL,
                `packetId` TEXT NOT NULL,
                `encodedPacket` BLOB NOT NULL,
                `attemptCount` INTEGER NOT NULL,
                `errorMessage` TEXT NOT NULL,
                `occurredAtEpochMilliseconds` INTEGER NOT NULL,
                PRIMARY KEY(`eventId`)
            )"""
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_protocol_outbox_failure_events_packetId_attemptCount` " +
                "ON `protocol_outbox_failure_events` (`packetId`, `attemptCount`)"
        )
        // Preserve failures that occurred before this journal existed. An automatic retry
        // must not erase the only durable evidence of the failed attempt during upgrade.
        connection.execSQL(
            """INSERT OR IGNORE INTO `protocol_outbox_failure_events`
                (`eventId`, `packetId`, `encodedPacket`, `attemptCount`, `errorMessage`, `occurredAtEpochMilliseconds`)
               SELECT `id` || ':' || `attemptCount`, `packetId`, `encodedPacket`, `attemptCount`,
                      COALESCE(`lastError`, 'Outgoing packet could not be sent'), `updatedAtEpochMilliseconds`
               FROM `protocol_outbox`
               WHERE `status` = 'FAILED'"""
        )
    }
}
