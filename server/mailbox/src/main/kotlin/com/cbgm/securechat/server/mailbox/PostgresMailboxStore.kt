package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.protocol.CreateMailboxRequest
import com.cbgm.securechat.server.protocol.CreateMailboxResponse
import com.cbgm.securechat.server.protocol.DeliveryRoute
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.serverJson
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection
import java.util.Base64

private data class MailboxCredentials(
    val mailboxId: String,
    val sendCapability: String,
    val retrievalCapability: String
)

internal class PostgresMailboxStore(
    private val database: PostgresMailboxDatabase,
    private val maximumEnvelopeBytes: Int,
    private val maximumMailboxBytes: Long,
    private val now: () -> Long = System::currentTimeMillis
) : MailboxStorage {
    private val secureRandom = SecureRandom()
    private val queries = PostgresMailboxQueries(now)

    override val persistenceMode: String = "postgresql"

    override suspend fun createWithQuota(
        request: CreateMailboxRequest,
        ownerKeyHash: String,
        maximumMailboxes: Int,
        maximumMailboxesPerOwner: Int
    ): MailboxCreationResult {
        require(request.expiresAtEpochMilliseconds > now())
        require(ownerKeyHash.isNotBlank())
        require(maximumMailboxes > 0)
        require(maximumMailboxesPerOwner > 0)

        return database.withConnection { connection ->
            connection.inMailboxTransaction {
                connection.createStatement().use { statement ->
                    statement.execute("LOCK TABLE mailboxes IN SHARE ROW EXCLUSIVE MODE")
                }
                queries.purgeExpiredMailboxes(connection)
                if (queries.countMailboxes(connection) >= maximumMailboxes) {
                    return@inMailboxTransaction MailboxCreationResult.GlobalQuotaExceeded
                }
                if (
                    queries.countMailboxes(connection, ownerKeyHash) >=
                    maximumMailboxesPerOwner
                ) {
                    return@inMailboxTransaction MailboxCreationResult.OwnerQuotaExceeded
                }

                repeat(MAXIMUM_IDENTIFIER_ATTEMPTS) {
                    val credentials =
                        MailboxCredentials(
                            mailboxId = randomToken(),
                            sendCapability = randomToken(),
                            retrievalCapability = randomToken()
                        )
                    if (insertMailbox(connection, request, ownerKeyHash, credentials)) {
                        return@inMailboxTransaction MailboxCreationResult.Created(
                            CreateMailboxResponse(
                                deliveryRoute =
                                    DeliveryRoute(
                                        routeId = randomToken(),
                                        nodeId = request.nodeId,
                                        nodeEndpoint = request.nodeEndpoint,
                                        mailboxId = credentials.mailboxId,
                                        sendCapability = credentials.sendCapability,
                                        sequence = request.routeSequence,
                                        expiresAtEpochMilliseconds =
                                            request.expiresAtEpochMilliseconds,
                                        identitySignature = byteArrayOf()
                                    ),
                                retrievalCapability = credentials.retrievalCapability
                            )
                        )
                    }
                }

                error("Could not allocate a unique mailbox identifier")
            }
        }
    }

    override suspend fun store(
        mailboxId: String,
        sendCapability: String,
        envelope: FederatedEnvelope
    ): MailboxResult =
        database.withConnection { connection ->
            connection.inMailboxTransaction {
                val mailbox =
                    queries.activeMailboxForUpdate(connection, mailboxId)
                        ?: return@inMailboxTransaction MailboxResult.Rejected("MAILBOX_NOT_FOUND")
                if (!matches(sendCapability, mailbox.sendCapabilityHash)) {
                    return@inMailboxTransaction MailboxResult.Rejected("INVALID_CAPABILITY")
                }
                if (envelope.expiresAtEpochMilliseconds <= now()) {
                    return@inMailboxTransaction MailboxResult.Rejected("ENVELOPE_EXPIRED")
                }
                val payloadBytes =
                    envelope.encryptedPayload
                        .encodeToByteArray()
                        .size
                        .toLong()
                if (payloadBytes > maximumEnvelopeBytes) {
                    return@inMailboxTransaction MailboxResult.Rejected("ENVELOPE_TOO_LARGE")
                }
                if (envelope.mailboxRoute?.mailboxId != mailboxId) {
                    return@inMailboxTransaction MailboxResult.Rejected("MAILBOX_ROUTE_MISMATCH")
                }

                queries.purgeExpiredEnvelopes(connection, mailboxId)
                if (queries.contains(connection, mailboxId, envelope.envelopeId)) {
                    return@inMailboxTransaction MailboxResult.Stored(duplicate = true)
                }
                if (queries.storedBytes(
                        connection,
                        mailboxId
                    ) + payloadBytes > maximumMailboxBytes
                ) {
                    return@inMailboxTransaction MailboxResult.Rejected("MAILBOX_QUOTA_EXCEEDED")
                }

                val inserted =
                    connection
                        .prepareStatement(
                            """
                            INSERT INTO mailbox_envelopes (
                                mailbox_id,
                                envelope_id,
                                envelope_json,
                                payload_bytes,
                                created_at_epoch_milliseconds,
                                expires_at_epoch_milliseconds
                            ) VALUES (?, ?, ?, ?, ?, ?)
                            ON CONFLICT (mailbox_id, envelope_id) DO NOTHING
                            """.trimIndent()
                        ).use { statement ->
                            statement.setString(EnvelopeInsertParameter.MAILBOX_ID, mailboxId)
                            statement.setString(
                                EnvelopeInsertParameter.ENVELOPE_ID,
                                envelope.envelopeId
                            )
                            statement.setString(
                                EnvelopeInsertParameter.ENVELOPE_JSON,
                                serverJson.encodeToString(envelope)
                            )
                            statement.setLong(EnvelopeInsertParameter.PAYLOAD_BYTES, payloadBytes)
                            statement.setLong(
                                EnvelopeInsertParameter.CREATED_AT,
                                envelope.createdAtEpochMilliseconds
                            )
                            statement.setLong(
                                EnvelopeInsertParameter.EXPIRES_AT,
                                envelope.expiresAtEpochMilliseconds
                            )
                            statement.executeUpdate() == 1
                        }
                MailboxResult.Stored(duplicate = !inserted)
            }
        }

    override suspend fun pending(
        mailboxId: String,
        retrievalCapability: String
    ): List<FederatedEnvelope>? =
        database.withConnection { connection ->
            connection.inMailboxTransaction {
                val mailbox =
                    queries.activeMailboxForUpdate(connection, mailboxId)
                        ?: return@inMailboxTransaction null
                if (!matches(retrievalCapability, mailbox.retrievalCapabilityHash)) {
                    return@inMailboxTransaction null
                }
                queries.purgeExpiredEnvelopes(connection, mailboxId)

                connection
                    .prepareStatement(
                        """
                        SELECT envelope_json
                        FROM mailbox_envelopes
                        WHERE mailbox_id = ?
                        ORDER BY created_at_epoch_milliseconds, envelope_id
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, mailboxId)
                        statement.executeQuery().use { results ->
                            buildList {
                                while (results.next()) {
                                    add(
                                        serverJson.decodeFromString<FederatedEnvelope>(
                                            results.getString(1)
                                        )
                                    )
                                }
                            }
                        }
                    }
            }
        }

    override suspend fun acknowledge(
        mailboxId: String,
        retrievalCapability: String,
        envelopeId: String
    ): Boolean =
        database.withConnection { connection ->
            connection.inMailboxTransaction {
                val mailbox =
                    queries.activeMailboxForUpdate(connection, mailboxId)
                        ?: return@inMailboxTransaction false
                if (!matches(retrievalCapability, mailbox.retrievalCapabilityHash)) {
                    return@inMailboxTransaction false
                }
                connection
                    .prepareStatement(
                        "DELETE FROM mailbox_envelopes WHERE mailbox_id = ? AND envelope_id = ?"
                    ).use { statement ->
                        statement.setString(1, mailboxId)
                        statement.setString(2, envelopeId)
                        statement.executeUpdate()
                    }
                true
            }
        }

    override suspend fun revoke(
        mailboxId: String,
        retrievalCapability: String
    ): MailboxRevocationResult =
        database.withConnection { connection ->
            connection.inMailboxTransaction {
                val mailbox =
                    queries.activeMailboxForUpdate(connection, mailboxId)
                        ?: return@inMailboxTransaction MailboxRevocationResult.NotFound
                if (!matches(retrievalCapability, mailbox.retrievalCapabilityHash)) {
                    return@inMailboxTransaction MailboxRevocationResult.Unauthorized
                }
                connection
                    .prepareStatement("DELETE FROM mailboxes WHERE mailbox_id = ?")
                    .use { statement ->
                        statement.setString(1, mailboxId)
                        statement.executeUpdate()
                    }
                MailboxRevocationResult.Revoked
            }
        }

    override suspend fun mailboxCount(): Int =
        database.withConnection { connection ->
            queries.purgeExpiredMailboxes(connection)
            connection.prepareStatement("SELECT COUNT(*) FROM mailboxes").use { statement ->
                statement.executeQuery().use { results ->
                    results.next()
                    results.getInt(1)
                }
            }
        }

    override fun close() {
        database.close()
    }

    private fun insertMailbox(
        connection: Connection,
        request: CreateMailboxRequest,
        ownerKeyHash: String,
        credentials: MailboxCredentials
    ): Boolean =
        connection
            .prepareStatement(
                """
                INSERT INTO mailboxes (
                    mailbox_id,
                    owner_key_hash,
                    send_capability_hash,
                    retrieval_capability_hash,
                    expires_at_epoch_milliseconds
                ) VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (mailbox_id) DO NOTHING
                """.trimIndent()
            ).use { statement ->
                statement.setString(MailboxInsertParameter.MAILBOX_ID, credentials.mailboxId)
                statement.setString(MailboxInsertParameter.OWNER_KEY_HASH, ownerKeyHash)
                statement.setBytes(
                    MailboxInsertParameter.SEND_CAPABILITY_HASH,
                    hash(credentials.sendCapability)
                )
                statement.setBytes(
                    MailboxInsertParameter.RETRIEVAL_CAPABILITY_HASH,
                    hash(credentials.retrievalCapability)
                )
                statement.setLong(
                    MailboxInsertParameter.EXPIRES_AT,
                    request.expiresAtEpochMilliseconds
                )
                statement.executeUpdate() == 1
            }

    private fun randomToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(value: String): ByteArray =
        MessageDigest.getInstance(CAPABILITY_HASH_ALGORITHM).digest(value.encodeToByteArray())

    private fun matches(
        capability: String,
        expectedHash: ByteArray
    ): Boolean = MessageDigest.isEqual(hash(capability), expectedHash)

    private object MailboxInsertParameter {
        const val MAILBOX_ID = 1
        const val OWNER_KEY_HASH = 2
        const val SEND_CAPABILITY_HASH = 3
        const val RETRIEVAL_CAPABILITY_HASH = 4
        const val EXPIRES_AT = 5
    }

    private object EnvelopeInsertParameter {
        const val MAILBOX_ID = 1
        const val ENVELOPE_ID = 2
        const val ENVELOPE_JSON = 3
        const val PAYLOAD_BYTES = 4
        const val CREATED_AT = 5
        const val EXPIRES_AT = 6
    }

    private companion object {
        const val TOKEN_BYTES = 32
        const val MAXIMUM_IDENTIFIER_ATTEMPTS = 5
        const val CAPABILITY_HASH_ALGORITHM = "SHA-256"
    }
}
