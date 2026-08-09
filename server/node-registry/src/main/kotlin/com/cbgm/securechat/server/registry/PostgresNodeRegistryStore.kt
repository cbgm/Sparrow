package com.cbgm.securechat.server.registry

import com.cbgm.securechat.server.protocol.NodeHeartbeatRequest
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.security.ProtocolSignatures
import java.sql.Connection
import java.sql.Types

internal class PostgresNodeRegistryStore(
    private val database: PostgresNodeRegistryDatabase,
    private val supportedProtocolVersions: Set<Int>,
    private val heartbeatGraceMilliseconds: Long,
    private val replayRetentionMilliseconds: Long,
    private val now: () -> Long = System::currentTimeMillis
) : NodeRegistryStorage {
    override val persistenceMode: String = "postgresql"

    override suspend fun register(descriptor: SecureChatNodeDescriptor): RegistrationResult {
        val currentTime = now()
        validateNodeDescriptor(descriptor, supportedProtocolVersions, currentTime)?.let { return it }

        database.withConnection { connection ->
            connection
                .prepareStatement(
                    """
                    INSERT INTO registered_nodes (
                        node_id,
                        descriptor_json,
                        valid_until_epoch_milliseconds,
                        last_heartbeat_at_epoch_milliseconds
                    ) VALUES (?, ?, ?, ?)
                    ON CONFLICT (node_id) DO UPDATE SET
                        descriptor_json = EXCLUDED.descriptor_json,
                        valid_until_epoch_milliseconds = EXCLUDED.valid_until_epoch_milliseconds,
                        last_heartbeat_at_epoch_milliseconds = EXCLUDED.last_heartbeat_at_epoch_milliseconds
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, descriptor.nodeId)
                    statement.setString(
                        2,
                        serverJson.encodeToString(
                            descriptor.copy(activeConnections = null)
                        )
                    )
                    statement.setLong(DESCRIPTOR_EXPIRATION_PARAMETER, descriptor.validUntilEpochMilliseconds)
                    statement.setLong(LAST_HEARTBEAT_PARAMETER, currentTime)
                    statement.executeUpdate()
                }
        }
        return RegistrationResult.Accepted
    }

    override suspend fun heartbeat(heartbeat: NodeHeartbeatRequest): RegistrationResult =
        database.withConnection { connection ->
            connection.inNodeRegistryTransaction {
                val descriptor =
                    findRegisteredForUpdate(connection, heartbeat.nodeId)
                        ?: return@inNodeRegistryTransaction RegistrationResult.Rejected("NODE_NOT_REGISTERED")
                val currentTime = now()
                if (!isHeartbeatFresh(heartbeat, replayRetentionMilliseconds, currentTime)) {
                    return@inNodeRegistryTransaction RegistrationResult.Rejected("STALE_OR_REPLAYED_HEARTBEAT")
                }
                purgeExpiredNonces(connection, currentTime)
                if (!recordNonce(connection, heartbeat, currentTime)) {
                    return@inNodeRegistryTransaction RegistrationResult.Rejected("STALE_OR_REPLAYED_HEARTBEAT")
                }
                if (!ProtocolSignatures.verifyHeartbeat(heartbeat, descriptor)) {
                    return@inNodeRegistryTransaction RegistrationResult.Rejected("INVALID_SIGNATURE")
                }

                connection
                    .prepareStatement(
                        """
                        UPDATE registered_nodes
                        SET last_heartbeat_at_epoch_milliseconds = ?,
                            active_connections = COALESCE(?, active_connections)
                        WHERE node_id = ?
                        """.trimIndent()
                    ).use { statement ->
                        statement.setLong(1, currentTime)
                        heartbeat.activeConnections?.let { activeConnections ->
                            statement.setInt(2, activeConnections)
                        } ?: statement.setNull(2, Types.INTEGER)
                        statement.setString(3, heartbeat.nodeId)
                        statement.executeUpdate()
                    }
                RegistrationResult.Accepted
            }
        }

    override suspend fun healthyNodes(): List<SecureChatNodeDescriptor> {
        val currentTime = now()
        return database.withConnection { connection ->
            connection
                .prepareStatement(
                    """
                    SELECT descriptor_json, active_connections
                    FROM registered_nodes
                    WHERE valid_until_epoch_milliseconds > ?
                      AND last_heartbeat_at_epoch_milliseconds >= ?
                    ORDER BY node_id
                    """.trimIndent()
                ).use { statement ->
                    statement.setLong(1, currentTime)
                    statement.setLong(2, currentTime - heartbeatGraceMilliseconds)
                    statement.executeQuery().use { results ->
                        buildList {
                            while (results.next()) {
                                add(
                                    serverJson
                                        .decodeFromString<SecureChatNodeDescriptor>(
                                            results.getString(1)
                                        ).copy(
                                            activeConnections = results.connectionLoad(2)
                                        )
                                )
                            }
                        }
                    }
                }
        }
    }

    override suspend fun findHealthy(nodeId: String): SecureChatNodeDescriptor? {
        val currentTime = now()
        return database.withConnection { connection ->
            connection
                .prepareStatement(
                    """
                    SELECT descriptor_json, active_connections
                    FROM registered_nodes
                    WHERE node_id = ?
                      AND valid_until_epoch_milliseconds > ?
                      AND last_heartbeat_at_epoch_milliseconds >= ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, nodeId)
                    statement.setLong(2, currentTime)
                    statement.setLong(HEARTBEAT_CUTOFF_PARAMETER, currentTime - heartbeatGraceMilliseconds)
                    statement.executeQuery().use { results ->
                        if (results.next()) {
                            serverJson
                                .decodeFromString<SecureChatNodeDescriptor>(
                                    results.getString(1)
                                ).copy(
                                    activeConnections = results.connectionLoad(2)
                                )
                        } else {
                            null
                        }
                    }
                }
        }
    }

    override fun close() {
        database.close()
    }

    private fun findRegisteredForUpdate(
        connection: Connection,
        nodeId: String
    ): SecureChatNodeDescriptor? =
        connection
            .prepareStatement(
                "SELECT descriptor_json FROM registered_nodes WHERE node_id = ? FOR UPDATE"
            ).use { statement ->
                statement.setString(1, nodeId)
                statement.executeQuery().use { results ->
                    if (results.next()) {
                        serverJson.decodeFromString<SecureChatNodeDescriptor>(results.getString(1))
                    } else {
                        null
                    }
                }
            }

    private fun purgeExpiredNonces(
        connection: Connection,
        currentTime: Long
    ) {
        connection
            .prepareStatement(
                "DELETE FROM node_heartbeat_nonces WHERE expires_at_epoch_milliseconds <= ?"
            ).use { statement ->
                statement.setLong(1, currentTime)
                statement.executeUpdate()
            }
    }

    private fun recordNonce(
        connection: Connection,
        heartbeat: NodeHeartbeatRequest,
        currentTime: Long
    ): Boolean =
        connection
            .prepareStatement(
                """
                INSERT INTO node_heartbeat_nonces (
                    node_id,
                    nonce,
                    expires_at_epoch_milliseconds
                ) VALUES (?, ?, ?)
                ON CONFLICT (node_id, nonce) DO NOTHING
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, heartbeat.nodeId)
                statement.setString(2, heartbeat.nonce)
                statement.setLong(
                    NONCE_EXPIRATION_PARAMETER,
                    currentTime + replayRetentionMilliseconds
                )
                statement.executeUpdate() == 1
            }

    private fun java.sql.ResultSet.connectionLoad(columnIndex: Int): Int? =
        getObject(columnIndex)?.let { value ->
            (value as Number).toInt()
        }

    private companion object {
        const val DESCRIPTOR_EXPIRATION_PARAMETER = 3
        const val LAST_HEARTBEAT_PARAMETER = 4
        const val HEARTBEAT_CUTOFF_PARAMETER = 3
        const val NONCE_EXPIRATION_PARAMETER = 3
    }
}
