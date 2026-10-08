package com.cbgm.sparrow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Moves structured message-part payloads onto the owning message-parts row. */
object MessagePartPayloadMigration53To54 : Migration(53, 54) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `message_parts` ADD COLUMN `payload` TEXT")
        connection.execSQL(
            """
            UPDATE `message_parts`
            SET `payload` = (
                SELECT `message_structured`.`json`
                FROM `message_structured`
                WHERE `message_structured`.`partId` = `message_parts`.`id`
            )
            WHERE EXISTS (
                SELECT 1
                FROM `message_structured`
                WHERE `message_structured`.`partId` = `message_parts`.`id`
            )
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE `message_structured`")
    }
}
