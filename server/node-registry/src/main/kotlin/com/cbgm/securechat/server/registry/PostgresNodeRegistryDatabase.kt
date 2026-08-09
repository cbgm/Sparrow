package com.cbgm.securechat.server.registry

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

internal data class PostgresNodeRegistryDatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int
) {
    init {
        require(jdbcUrl.isNotBlank()) {
            "Node registry database JDBC URL must not be blank"
        }
        require(maximumPoolSize > 0) {
            "Node registry database maximum pool size must be positive"
        }
    }
}

internal class PostgresNodeRegistryDatabase(
    config: PostgresNodeRegistryDatabaseConfig
) : AutoCloseable {
    private val dataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.jdbcUrl
                username = config.username
                password = config.password
                maximumPoolSize = config.maximumPoolSize
                minimumIdle = 1
                connectionTimeout = DATABASE_CONNECTION_TIMEOUT_MILLISECONDS
                validationTimeout = DATABASE_VALIDATION_TIMEOUT_MILLISECONDS
                initializationFailTimeout = DATABASE_INITIALIZATION_TIMEOUT_MILLISECONDS
                poolName = "securechat-node-registry"
            }
        )

    init {
        initializeSchema()
    }

    suspend fun <T> withConnection(block: (Connection) -> T): T =
        withContext(Dispatchers.IO) {
            dataSource.connection.use(block)
        }

    override fun close() {
        dataSource.close()
    }

    private fun initializeSchema() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                NODE_REGISTRY_SCHEMA_STATEMENTS.forEach { sql ->
                    statement.execute(sql)
                }
            }
        }
    }

    private companion object {
        const val DATABASE_CONNECTION_TIMEOUT_MILLISECONDS = 10_000L
        const val DATABASE_VALIDATION_TIMEOUT_MILLISECONDS = 5_000L
        const val DATABASE_INITIALIZATION_TIMEOUT_MILLISECONDS = 60_000L

        val NODE_REGISTRY_SCHEMA_STATEMENTS =
            listOf(
                """
                CREATE TABLE IF NOT EXISTS registered_nodes (
                    node_id TEXT PRIMARY KEY,
                    descriptor_json TEXT NOT NULL,
                    valid_until_epoch_milliseconds BIGINT NOT NULL,
                    last_heartbeat_at_epoch_milliseconds BIGINT NOT NULL,
                    active_connections INTEGER
                )
                """.trimIndent(),
                """
                ALTER TABLE registered_nodes
                ADD COLUMN IF NOT EXISTS active_connections INTEGER
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS registered_nodes_health_idx
                ON registered_nodes (
                    valid_until_epoch_milliseconds,
                    last_heartbeat_at_epoch_milliseconds
                )
                """.trimIndent(),
                """
                CREATE TABLE IF NOT EXISTS node_heartbeat_nonces (
                    node_id TEXT NOT NULL,
                    nonce TEXT NOT NULL,
                    expires_at_epoch_milliseconds BIGINT NOT NULL,
                    PRIMARY KEY (node_id, nonce)
                )
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS node_heartbeat_nonces_expiry_idx
                ON node_heartbeat_nonces (expires_at_epoch_milliseconds)
                """.trimIndent()
            )
    }
}

internal fun <T> Connection.inNodeRegistryTransaction(block: () -> T): T {
    val previousAutoCommit = autoCommit
    autoCommit = false

    return try {
        runCatching {
            val result = block()
            commit()
            result
        }.onFailure {
            rollback()
        }.getOrThrow()
    } finally {
        autoCommit = previousAutoCommit
    }
}
