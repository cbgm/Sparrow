package com.cbgm.sparrow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object InvitationPeerDetailsMigration44To45 : Migration(44, 45) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `invitations` ADD COLUMN `peerDisplayName` TEXT"
        )
        connection.execSQL(
            "ALTER TABLE `invitations` ADD COLUMN `peerSecondaryText` TEXT"
        )
    }
}
