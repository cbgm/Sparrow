package com.cbgm.securechat.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object DatabaseMigrations {
    val Migration9To10 =
        object : Migration(9, 10) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("PRAGMA defer_foreign_keys = ON")

                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conversations_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        contactId TEXT,
                        type TEXT NOT NULL,
                        title TEXT,
                        createdAtEpochMilliseconds INTEGER NOT NULL,
                        updatedAtEpochMilliseconds INTEGER NOT NULL,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                connection.execSQL(
                    """
                    INSERT INTO conversations_new (
                        id,
                        contactId,
                        type,
                        title,
                        createdAtEpochMilliseconds,
                        updatedAtEpochMilliseconds
                    )
                    SELECT
                        id,
                        contactId,
                        'DIRECT',
                        NULL,
                        createdAtEpochMilliseconds,
                        updatedAtEpochMilliseconds
                    FROM conversations
                    """.trimIndent()
                )

                connection.execSQL("DROP TABLE conversations")
                connection.execSQL("ALTER TABLE conversations_new RENAME TO conversations")
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_conversations_contactId " +
                        "ON conversations(contactId)"
                )
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_type ON conversations(type)")
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conversations_updatedAtEpochMilliseconds " +
                        "ON conversations(updatedAtEpochMilliseconds)"
                )

                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conversation_participants (
                        conversationId TEXT NOT NULL,
                        contactId TEXT NOT NULL,
                        role TEXT NOT NULL,
                        joinedAtEpochMilliseconds INTEGER NOT NULL,
                        PRIMARY KEY(conversationId, contactId),
                        FOREIGN KEY(conversationId) REFERENCES conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conversation_participants_conversationId " +
                        "ON conversation_participants(conversationId)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conversation_participants_contactId " +
                        "ON conversation_participants(contactId)"
                )
                connection.execSQL(
                    """
                    INSERT OR IGNORE INTO conversation_participants (
                        conversationId,
                        contactId,
                        role,
                        joinedAtEpochMilliseconds
                    )
                    SELECT
                        id,
                        contactId,
                        'MEMBER',
                        createdAtEpochMilliseconds
                    FROM conversations
                    WHERE contactId IS NOT NULL
                    """.trimIndent()
                )

                connection.execSQL("ALTER TABLE messages ADD COLUMN senderContactId TEXT")
            }
        }

    val Migration10To11 =
        object : Migration(10, 11) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS message_recipient_states (
                        messageId TEXT NOT NULL,
                        contactId TEXT NOT NULL,
                        packetId TEXT,
                        deliveryStatus TEXT NOT NULL,
                        lastError TEXT,
                        updatedAtEpochMilliseconds INTEGER NOT NULL,
                        PRIMARY KEY(messageId, contactId),
                        FOREIGN KEY(messageId) REFERENCES messages(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_message_recipient_states_messageId " +
                        "ON message_recipient_states(messageId)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_message_recipient_states_contactId " +
                        "ON message_recipient_states(contactId)"
                )
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_message_recipient_states_packetId " +
                        "ON message_recipient_states(packetId)"
                )
            }
        }

    val Migration11To12 =
        object : Migration(11, 12) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS contact_relay_ids (
                        contactId TEXT NOT NULL PRIMARY KEY,
                        relayId TEXT NOT NULL,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_contact_relay_ids_contactId " +
                        "ON contact_relay_ids(contactId)"
                )
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_contact_relay_ids_relayId " +
                        "ON contact_relay_ids(relayId)"
                )
            }
        }
    val Migration12To13 =
        object : Migration(12, 13) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE contact_public_identities " +
                        "ADD COLUMN locallyImported INTEGER NOT NULL DEFAULT 0"
                )
                connection.execSQL(
                    "ALTER TABLE contact_public_identities " +
                        "ADD COLUMN remoteIdentityPacketReceived INTEGER NOT NULL DEFAULT 0"
                )
                connection.execSQL(
                    "UPDATE contact_public_identities " +
                        "SET keyExchangeStatus = 'ONE_WAY', " +
                        "verificationStatus = 'UNVERIFIED'"
                )
            }
        }

    val Migration13To14 =
        object : Migration(13, 14) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS group_security_states (
                        groupId TEXT NOT NULL PRIMARY KEY,
                        currentEpoch INTEGER NOT NULL,
                        welcomePacketId TEXT,
                        ownerContactId TEXT,
                        ownerSigningPublicKey BLOB NOT NULL,
                        localSigningPublicKey BLOB NOT NULL,
                        updatedAtEpochMilliseconds INTEGER NOT NULL,
                        FOREIGN KEY(groupId) REFERENCES conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS group_member_keys (
                        groupId TEXT NOT NULL,
                        epoch INTEGER NOT NULL,
                        contactId TEXT NOT NULL,
                        encryptionPublicKey BLOB NOT NULL,
                        signingPublicKey BLOB NOT NULL,
                        role TEXT NOT NULL,
                        PRIMARY KEY(groupId, epoch, contactId),
                        FOREIGN KEY(groupId) REFERENCES conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_group_member_keys_groupId " +
                        "ON group_member_keys(groupId)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_group_member_keys_contactId " +
                        "ON group_member_keys(contactId)"
                )
            }
        }

    val Migration14To15 =
        object : Migration(14, 15) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS group_invitations (
                        invitationId TEXT NOT NULL PRIMARY KEY,
                        groupId TEXT NOT NULL,
                        contactId TEXT NOT NULL,
                        status TEXT NOT NULL,
                        challenge BLOB NOT NULL,
                        createdAtEpochMilliseconds INTEGER NOT NULL,
                        expiresAtEpochMilliseconds INTEGER NOT NULL,
                        updatedAtEpochMilliseconds INTEGER NOT NULL,
                        FOREIGN KEY(groupId) REFERENCES conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_group_invitations_groupId " +
                        "ON group_invitations(groupId)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_group_invitations_contactId " +
                        "ON group_invitations(contactId)"
                )
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_group_invitations_groupId_contactId " +
                        "ON group_invitations(groupId, contactId)"
                )
            }
        }
    val Migration15To16 =
        object : Migration(15, 16) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS identity_invitations (
                        invitationId TEXT NOT NULL PRIMARY KEY,
                        contactId TEXT NOT NULL,
                        direction TEXT NOT NULL,
                        state TEXT NOT NULL,
                        remoteDisplayName TEXT,
                        inviteChallenge BLOB NOT NULL,
                        responseChallenge BLOB,
                        remoteEncryptionPublicKey BLOB NOT NULL,
                        remoteSigningPublicKey BLOB NOT NULL,
                        createdAtEpochMilliseconds INTEGER NOT NULL,
                        expiresAtEpochMilliseconds INTEGER NOT NULL,
                        updatedAtEpochMilliseconds INTEGER NOT NULL,
                        lastError TEXT,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_identity_invitations_contactId " +
                        "ON identity_invitations(contactId)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_identity_invitations_direction " +
                        "ON identity_invitations(direction)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_identity_invitations_state " +
                        "ON identity_invitations(state)"
                )
            }
        }

    val Migration16To17 =
        object : Migration(16, 17) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE contact_public_identities " +
                        "ADD COLUMN verifiedByContact INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

    val Migration17To18 =
        object : Migration(17, 18) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS group_verification_pairs (
                        groupId TEXT NOT NULL,
                        invitationId TEXT NOT NULL,
                        contactId TEXT,
                        displayName TEXT NOT NULL,
                        membershipStatus TEXT NOT NULL,
                        participantEncryptionPublicKey BLOB,
                        participantSigningPublicKey BLOB,
                        adminVerifiedParticipant INTEGER NOT NULL,
                        participantVerifiedAdmin INTEGER NOT NULL,
                        updatedAtEpochMilliseconds INTEGER NOT NULL,
                        PRIMARY KEY(groupId, invitationId),
                        FOREIGN KEY(groupId) REFERENCES conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_group_verification_pairs_groupId " +
                        "ON group_verification_pairs(groupId)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_group_verification_pairs_contactId " +
                        "ON group_verification_pairs(contactId)"
                )
            }
        }

    val Migration18To19 =
        object : Migration(18, 19) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    UPDATE group_invitations
                    SET updatedAtEpochMilliseconds = createdAtEpochMilliseconds
                    WHERE updatedAtEpochMilliseconds < createdAtEpochMilliseconds
                    """.trimIndent()
                )
                connection.execSQL(
                    """
                    UPDATE conversations
                    SET updatedAtEpochMilliseconds = createdAtEpochMilliseconds
                    WHERE updatedAtEpochMilliseconds < createdAtEpochMilliseconds
                    """.trimIndent()
                )
            }
        }

    val Migration19To20 =
        object : Migration(19, 20) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE group_invitations " +
                        "ADD COLUMN direction TEXT NOT NULL DEFAULT 'INCOMING'"
                )
                connection.execSQL(
                    """
                    UPDATE group_invitations
                    SET direction = 'OUTGOING'
                    WHERE status IN (
                        'INVITE_SENT',
                        'WAITING_FOR_IDENTITY',
                        'IDENTITY_READY',
                        'WELCOME_SENT'
                    )
                       OR EXISTS (
                            SELECT 1
                            FROM group_security_states
                            WHERE group_security_states.groupId = group_invitations.groupId
                              AND group_security_states.ownerContactId IS NULL
                       )
                       OR EXISTS (
                            SELECT 1
                            FROM group_verification_pairs
                            WHERE group_verification_pairs.groupId = group_invitations.groupId
                              AND group_verification_pairs.contactId IS NOT NULL
                       )
                       OR (
                            SELECT COUNT(*)
                            FROM group_invitations AS group_rows
                            WHERE group_rows.groupId = group_invitations.groupId
                       ) > 1
                       OR EXISTS (
                            SELECT 1
                            FROM protocol_outbox
                            WHERE protocol_outbox.packetId =
                                'group-invite-' || group_invitations.invitationId
                       )
                    """.trimIndent()
                )
            }
        }

    val Migration20To21 =
        object : Migration(20, 21) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_mailbox_credentials (
                        contactId TEXT NOT NULL PRIMARY KEY,
                        routeId TEXT NOT NULL,
                        nodeId TEXT NOT NULL,
                        nodeEndpoint TEXT NOT NULL,
                        mailboxId TEXT NOT NULL,
                        sendCapability TEXT NOT NULL,
                        accessEndpoint TEXT NOT NULL,
                        retrievalCapability TEXT NOT NULL,
                        sequence INTEGER NOT NULL,
                        expiresAtEpochMilliseconds INTEGER NOT NULL,
                        identitySignature BLOB NOT NULL,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_local_mailbox_credentials_contactId " +
                        "ON local_mailbox_credentials(contactId)"
                )
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS remote_mailbox_routes (
                        contactId TEXT NOT NULL PRIMARY KEY,
                        routeId TEXT NOT NULL,
                        nodeId TEXT NOT NULL,
                        nodeEndpoint TEXT NOT NULL,
                        mailboxId TEXT NOT NULL,
                        sendCapability TEXT NOT NULL,
                        sequence INTEGER NOT NULL,
                        expiresAtEpochMilliseconds INTEGER NOT NULL,
                        identitySignature BLOB NOT NULL,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_remote_mailbox_routes_contactId " +
                        "ON remote_mailbox_routes(contactId)"
                )
            }
        }

    val Migration21To22 =
        object : Migration(21, 22) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE local_mailbox_credentials " +
                        "ADD COLUMN revocationPending INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

    val Migration22To23 =
        object : Migration(22, 23) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE group_invitations " +
                        "ADD COLUMN ownerEncryptionPublicKey BLOB"
                )
                connection.execSQL(
                    "ALTER TABLE group_invitations " +
                        "ADD COLUMN ownerSigningPublicKey BLOB"
                )
            }
        }
    val Migration23To24 =
        object : Migration(23, 24) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE identity_invitations " +
                        "ADD COLUMN localEncryptionPublicKey BLOB"
                )
                connection.execSQL(
                    "ALTER TABLE identity_invitations " +
                        "ADD COLUMN localSigningPublicKey BLOB"
                )
            }
        }
}
