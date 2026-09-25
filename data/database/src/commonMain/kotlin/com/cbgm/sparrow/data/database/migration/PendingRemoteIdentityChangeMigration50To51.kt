package com.cbgm.sparrow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Persist out-of-band key confirmation without altering old contact trust or conversation data. */
object PendingRemoteIdentityChangeMigration50To51 : Migration(50, 51) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `pending_remote_identity_changes` ADD COLUMN `fingerprintConfirmedAtEpochMilliseconds` INTEGER")
        connection.execSQL("ALTER TABLE `pending_remote_identity_changes` ADD COLUMN `confirmedPreviousEncryptionPublicKey` BLOB")
        connection.execSQL("ALTER TABLE `pending_remote_identity_changes` ADD COLUMN `confirmedPreviousSigningPublicKey` BLOB")
    }
}
