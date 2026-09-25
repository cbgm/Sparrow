package com.cbgm.sparrow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object IdentityExchangeMigration43To44 : Migration(43, 44) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `identity_exchanges` ADD COLUMN `wasKnownPeerAtReceive` INTEGER"
        )
    }
}
