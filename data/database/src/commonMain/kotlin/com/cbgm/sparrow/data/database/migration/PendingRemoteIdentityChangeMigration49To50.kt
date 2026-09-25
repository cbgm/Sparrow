package com.cbgm.sparrow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Adds a separate, non-authorizing inbox for remote-identity change candidates. */
object PendingRemoteIdentityChangeMigration49To50 : Migration(49, 50) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS `pending_remote_identity_changes` (
                `peerId` TEXT NOT NULL,
                `sourcePeerId` TEXT NOT NULL,
                `invitationId` TEXT NOT NULL,
                `proposedEncryptionPublicKey` BLOB NOT NULL,
                `proposedSigningPublicKey` BLOB NOT NULL,
                `receivedAtEpochMilliseconds` INTEGER NOT NULL,
                `expiresAtEpochMilliseconds` INTEGER NOT NULL,
                PRIMARY KEY(`peerId`),
                FOREIGN KEY(`peerId`) REFERENCES `contacts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )"""
        )
    }
}
