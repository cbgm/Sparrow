package com.cbgm.sparrow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Backfill cursor timestamp for persisted attachment contexts on existing installations. */
object AttachmentMessageContextMigration46To47 : Migration(46, 47) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `attachment_message_contexts` ADD COLUMN `createdAtEpochMilliseconds` INTEGER NOT NULL DEFAULT 0"
        )
        connection.execSQL(
            """UPDATE attachment_message_contexts
               SET createdAtEpochMilliseconds = (
                   SELECT messages.createdAtEpochMilliseconds FROM messages
                   WHERE messages.id = attachment_message_contexts.messageId
               )"""
        )
    }
}
