package com.cbgm.sparrow.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Backfills attachment-owned context for all existing installations, including on-demand loads. */
object AttachmentMessageContextMigration45To46 : Migration(45, 46) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS `attachment_message_contexts` (
                `messageId` TEXT NOT NULL,
                `conversationId` TEXT NOT NULL,
                `displayName` TEXT NOT NULL,
                `isGroup` INTEGER NOT NULL,
                `isMine` INTEGER NOT NULL,
                `senderContactId` TEXT,
                PRIMARY KEY(`messageId`),
                FOREIGN KEY(`messageId`) REFERENCES `messages`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )"""
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_attachment_message_contexts_conversationId` ON `attachment_message_contexts` (`conversationId`)"
        )
        connection.execSQL(
            """INSERT INTO attachment_message_contexts
                (messageId, conversationId, displayName, isGroup, isMine, senderContactId)
            SELECT DISTINCT m.id, m.conversationId,
                CASE WHEN c.type = 'GROUP' THEN
                    COALESCE(NULLIF(TRIM(c.title), ''), c.id)
                ELSE
                    COALESCE(NULLIF(TRIM(c.title), ''), NULLIF(TRIM(p.displayName), ''),
                        (SELECT n.value FROM contact_phone_numbers AS n
                         WHERE n.id = p.preferredPhoneNumberId LIMIT 1),
                        (SELECT n.value FROM contact_phone_numbers AS n
                         WHERE n.contactId = c.contactId
                         ORDER BY n.updatedAtEpochMilliseconds DESC LIMIT 1),
                        c.contactId, c.id)
                END,
                CASE WHEN c.type = 'GROUP' THEN 1 ELSE 0 END,
                m.isMine, m.senderContactId
            FROM message_attachments AS a
            INNER JOIN messages AS m ON m.id = a.messageId
            INNER JOIN conversations AS c ON c.id = m.conversationId
            LEFT JOIN contacts AS p ON p.id = c.contactId"""
        )
    }
}
