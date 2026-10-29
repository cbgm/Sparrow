package com.cbgm.sparrow.server.mailbox

import com.cbgm.sparrow.server.protocol.MailboxWakeTicket
import com.cbgm.sparrow.server.protocol.serverJson
import java.sql.Connection

internal data class MailboxAuthorization(
    val sendCapabilityHash: ByteArray,
    val retrievalCapabilityHash: ByteArray,
    val wakeTicket: MailboxWakeTicket? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MailboxAuthorization

        if (!sendCapabilityHash.contentEquals(other.sendCapabilityHash)) return false
        if (!retrievalCapabilityHash.contentEquals(other.retrievalCapabilityHash)) return false
        if (wakeTicket != other.wakeTicket) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sendCapabilityHash.contentHashCode()
        result = 31 * result + retrievalCapabilityHash.contentHashCode()
        result = 31 * result + (wakeTicket?.hashCode() ?: 0)
        return result
    }
}

internal class PostgresMailboxQueries(
    private val now: () -> Long
) {
    fun activeMailboxForUpdate(
        connection: Connection,
        mailboxId: String
    ): MailboxAuthorization? {
        purgeExpiredMailbox(connection, mailboxId)
        return connection
            .prepareStatement(
                """
                SELECT send_capability_hash, retrieval_capability_hash, wake_ticket_json
                FROM mailboxes
                WHERE mailbox_id = ? AND expires_at_epoch_milliseconds > ?
                FOR UPDATE
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, mailboxId)
                statement.setLong(2, now())
                statement.executeQuery().use { results ->
                    if (results.next()) {
                        MailboxAuthorization(
                            sendCapabilityHash = results.getBytes(1),
                            retrievalCapabilityHash = results.getBytes(2),
                            wakeTicket =
                                results.getString(3)?.let { encoded ->
                                    serverJson.decodeFromString<MailboxWakeTicket>(encoded)
                                }
                        )
                    } else {
                        null
                    }
                }
            }
    }

    fun purgeExpiredMailboxes(connection: Connection) {
        connection
            .prepareStatement("DELETE FROM mailboxes WHERE expires_at_epoch_milliseconds <= ?")
            .use { statement ->
                statement.setLong(1, now())
                statement.executeUpdate()
            }
    }

    fun countMailboxes(
        connection: Connection,
        ownerKeyHash: String? = null
    ): Int {
        val sql =
            if (ownerKeyHash == null) {
                "SELECT COUNT(*) FROM mailboxes"
            } else {
                "SELECT COUNT(*) FROM mailboxes WHERE owner_key_hash = ?"
            }
        return connection.prepareStatement(sql).use { statement ->
            ownerKeyHash?.let { statement.setString(1, it) }
            statement.executeQuery().use { results ->
                results.next()
                results.getInt(1)
            }
        }
    }

    fun purgeExpiredEnvelopes(
        connection: Connection,
        mailboxId: String
    ) {
        connection
            .prepareStatement(
                """
                DELETE FROM mailbox_envelopes
                WHERE mailbox_id = ? AND expires_at_epoch_milliseconds <= ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, mailboxId)
                statement.setLong(2, now())
                statement.executeUpdate()
            }
    }

    fun contains(
        connection: Connection,
        mailboxId: String,
        envelopeId: String
    ): Boolean =
        connection
            .prepareStatement(
                "SELECT 1 FROM mailbox_envelopes WHERE mailbox_id = ? AND envelope_id = ?"
            ).use { statement ->
                statement.setString(1, mailboxId)
                statement.setString(2, envelopeId)
                statement.executeQuery().use { results -> results.next() }
            }

    fun storedBytes(
        connection: Connection,
        mailboxId: String
    ): Long =
        connection
            .prepareStatement(
                "SELECT COALESCE(SUM(payload_bytes), 0) FROM mailbox_envelopes WHERE mailbox_id = ?"
            ).use { statement ->
                statement.setString(1, mailboxId)
                statement.executeQuery().use { results ->
                    results.next()
                    results.getLong(1)
                }
            }

    private fun purgeExpiredMailbox(
        connection: Connection,
        mailboxId: String
    ) {
        connection
            .prepareStatement(
                """
                DELETE FROM mailboxes
                WHERE mailbox_id = ? AND expires_at_epoch_milliseconds <= ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, mailboxId)
                statement.setLong(2, now())
                statement.executeUpdate()
            }
    }
}
