package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.protocol.TransportEnvelope
import java.sql.Connection

internal class PostgresPendingEnvelopeStore(
    private val database: PostgresPushDatabase,
    private val maximumEnvelopes: Int,
    private val retentionMilliseconds: Long,
    private val now: () -> Long = System::currentTimeMillis
) : PendingEnvelopeStore {
    init {
        require(maximumEnvelopes > 0) {
            "Maximum envelope count must be positive"
        }
        require(retentionMilliseconds > 0L) {
            "Envelope retention must be positive"
        }
    }

    override suspend fun enqueue(envelope: TransportEnvelope): Boolean =
        database.withConnection { connection ->
            connection.inTransaction {
                connection
                    .prepareStatement("SELECT pg_advisory_xact_lock(?)")
                    .use { statement ->
                        statement.setLong(1, ENVELOPE_CAPACITY_LOCK_ID)
                        statement.execute()
                    }

                purgeExpired(connection)

                if (contains(connection, envelope.envelopeId)) {
                    return@inTransaction false
                }

                if (count(connection) >= maximumEnvelopes) {
                    return@inTransaction false
                }

                connection
                    .prepareStatement(
                        """
                        INSERT INTO pending_envelopes (
                            envelope_id,
                            version,
                            sender_id,
                            recipient_id,
                            payload,
                            created_at_epoch_milliseconds,
                            expires_at_epoch_milliseconds
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (envelope_id) DO NOTHING
                        """.trimIndent()
                    ).use { statement ->
                        var parameterIndex = 1
                        statement.setString(parameterIndex++, envelope.envelopeId)
                        statement.setInt(parameterIndex++, envelope.version)
                        statement.setString(parameterIndex++, envelope.senderId)
                        statement.setString(parameterIndex++, envelope.recipientId)
                        statement.setString(parameterIndex++, envelope.payload)
                        statement.setLong(parameterIndex++, envelope.createdAtEpochMilliseconds)
                        statement.setLong(parameterIndex, now() + retentionMilliseconds)
                        statement.executeUpdate() == 1
                    }
            }
        }

    override suspend fun pending(recipientId: String): List<TransportEnvelope> =
        database.withConnection { connection ->
            purgeExpired(connection)

            connection
                .prepareStatement(
                    """
                    SELECT
                        version,
                        envelope_id,
                        sender_id,
                        recipient_id,
                        payload,
                        created_at_epoch_milliseconds
                    FROM pending_envelopes
                    WHERE recipient_id = ?
                    ORDER BY created_at_epoch_milliseconds, envelope_id
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, recipientId)
                    statement.executeQuery().use { results ->
                        results.readTransportEnvelopes()
                    }
                }
        }

    override suspend fun remove(
        recipientId: String,
        envelopeId: String
    ) {
        database.withConnection { connection ->
            connection
                .prepareStatement(
                    """
                    DELETE FROM pending_envelopes
                    WHERE recipient_id = ? AND envelope_id = ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, recipientId)
                    statement.setString(2, envelopeId)
                    statement.executeUpdate()
                }
        }
    }

    override suspend fun contains(envelopeId: String): Boolean =
        database.withConnection { connection ->
            purgeExpired(connection)
            contains(connection, envelopeId)
        }

    override suspend fun pendingRecipientIds(): Set<String> =
        database.withConnection { connection ->
            purgeExpired(connection)

            connection
                .prepareStatement(
                    """
                    SELECT DISTINCT recipient_id
                    FROM pending_envelopes
                    ORDER BY recipient_id
                    """.trimIndent()
                ).use { statement ->
                    statement.executeQuery().use { results ->
                        results.readRecipientIds()
                    }
                }
        }

    override suspend fun count(): Int =
        database.withConnection { connection ->
            purgeExpired(connection)
            count(connection)
        }

    private fun contains(
        connection: Connection,
        envelopeId: String
    ): Boolean =
        connection
            .prepareStatement(
                "SELECT 1 FROM pending_envelopes WHERE envelope_id = ?"
            ).use { statement ->
                statement.setString(1, envelopeId)
                statement.executeQuery().use { results ->
                    results.next()
                }
            }

    private fun count(connection: Connection): Int =
        connection
            .prepareStatement("SELECT COUNT(*) FROM pending_envelopes")
            .use { statement ->
                statement.executeQuery().use { results ->
                    results.next()
                    results.getInt(1)
                }
            }

    private fun purgeExpired(connection: Connection) {
        connection
            .prepareStatement(
                "DELETE FROM pending_envelopes WHERE expires_at_epoch_milliseconds <= ?"
            ).use { statement ->
                statement.setLong(1, now())
                statement.executeUpdate()
            }
    }

    private companion object {
        const val ENVELOPE_CAPACITY_LOCK_ID = 7_041_953_127L
    }
}
