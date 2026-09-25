package com.cbgm.sparrow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Persist verified ORIGINAL invitation facts for one-tap acceptance after restart.
 * Nullable fields preserve old v52 approvals: they fall back to fresh invitations. */
object ApprovedIdentityOfferMigration52To53 : Migration(52, 53) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `pending_remote_identity_changes` ADD COLUMN `originalInviteChallenge` BLOB")
        connection.execSQL("ALTER TABLE `pending_remote_identity_changes` ADD COLUMN `originalInviteCreatedAtEpochMilliseconds` INTEGER")
        connection.execSQL("ALTER TABLE `pending_remote_identity_changes` ADD COLUMN `originalInviteAutoSharesIdentity` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE `approved_identity_reconnections` ADD COLUMN `originalInviteChallenge` BLOB")
        connection.execSQL("ALTER TABLE `approved_identity_reconnections` ADD COLUMN `originalInviteCreatedAtEpochMilliseconds` INTEGER")
        connection.execSQL("ALTER TABLE `approved_identity_reconnections` ADD COLUMN `originalInviteExpiresAtEpochMilliseconds` INTEGER")
        connection.execSQL("ALTER TABLE `approved_identity_reconnections` ADD COLUMN `originalInviteAutoSharesIdentity` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE `approved_identity_reconnections` ADD COLUMN `originalInviterEncryptionPublicKey` BLOB")
        connection.execSQL("ALTER TABLE `approved_identity_reconnections` ADD COLUMN `originalInviterSigningPublicKey` BLOB")
    }
}
