package com.cbgm.sparrow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Preserve existing chats while adding an atomic post-approval recovery handoff. */
object ApprovedIdentityReconnectionMigration51To52 : Migration(51, 52) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `approved_identity_reconnections` (
                `peerId` TEXT NOT NULL,
                `approvalId` TEXT NOT NULL,
                `approvedAtEpochMilliseconds` INTEGER NOT NULL,
                PRIMARY KEY(`peerId`),
                FOREIGN KEY(`peerId`) REFERENCES `contacts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}
