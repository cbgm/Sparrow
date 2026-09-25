package com.cbgm.sparrow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Preserve existing legitimate phone contacts once; later welcomes reuse the group epoch roster. */
object GroupMemberPhoneMigration47To48 : Migration(47, 48) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `group_member_keys` ADD COLUMN `phoneNumber` TEXT")
        connection.execSQL(
            """UPDATE group_member_keys
               SET phoneNumber = (
                   SELECT p.value FROM contact_phone_numbers p
                   LEFT JOIN contacts c ON c.id = p.contactId
                   WHERE p.contactId = group_member_keys.contactId
                     AND TRIM(p.value) <> ''
                   ORDER BY CASE WHEN p.id = c.preferredPhoneNumberId THEN 0 ELSE 1 END, p.id
                   LIMIT 1
               )"""
        )
    }
}
