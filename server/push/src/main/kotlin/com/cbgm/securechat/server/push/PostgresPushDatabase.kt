package com.cbgm.securechat.server.push

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

internal data class PostgresPushDatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int
) {
    init {
        require(jdbcUrl.isNotBlank()) {
            "Push database JDBC URL must not be blank"
        }
        require(maximumPoolSize > 0) {
            "Push database maximum pool size must be positive"
        }
    }
}

internal class PostgresPushDatabase(
    config: PostgresPushDatabaseConfig
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
                poolName = "securechat-push"
            }
        )

    init {
        initializeSchema()
    }

    suspend fun <T> withConnection(
        block: (Connection) -> T
    ): T =
        withContext(Dispatchers.IO) {
            dataSource.connection.use(block)
        }

    override fun close() {
        dataSource.close()
    }

    private fun initializeSchema() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                PUSH_SCHEMA_STATEMENTS.forEach { sql ->
                    statement.execute(sql)
                }
            }
        }
    }

    private companion object {
        const val DATABASE_CONNECTION_TIMEOUT_MILLISECONDS = 10_000L
        const val DATABASE_VALIDATION_TIMEOUT_MILLISECONDS = 5_000L
        const val DATABASE_INITIALIZATION_TIMEOUT_MILLISECONDS = 60_000L

        val PUSH_SCHEMA_STATEMENTS =
            listOf(
                """
                CREATE TABLE IF NOT EXISTS push_devices (
                    token TEXT PRIMARY KEY,
                    routing_id TEXT NOT NULL,
                    platform TEXT NOT NULL,
                    updated_at_epoch_milliseconds BIGINT NOT NULL
                )
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS push_devices_routing_id_idx
                ON push_devices (routing_id)
                """.trimIndent(),
                """
                CREATE TABLE IF NOT EXISTS pending_envelopes (
                    envelope_id TEXT PRIMARY KEY,
                    version INTEGER NOT NULL,
                    sender_id TEXT NOT NULL,
                    recipient_id TEXT NOT NULL,
                    payload TEXT NOT NULL,
                    created_at_epoch_milliseconds BIGINT NOT NULL,
                    expires_at_epoch_milliseconds BIGINT NOT NULL
                )
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS pending_envelopes_recipient_idx
                ON pending_envelopes (recipient_id, created_at_epoch_milliseconds)
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS pending_envelopes_expiry_idx
                ON pending_envelopes (expires_at_epoch_milliseconds)
                """.trimIndent(),
                """
                CREATE TABLE IF NOT EXISTS push_wake_ups (
                    wake_up_id TEXT PRIMARY KEY,
                    recipient_id TEXT NOT NULL,
                    expires_at_epoch_milliseconds BIGINT NOT NULL
                )
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS push_wake_ups_expiry_idx
                ON push_wake_ups (expires_at_epoch_milliseconds)
                """.trimIndent()
            )
    }
}

internal fun <T> Connection.inTransaction(
    block: () -> T
): T {
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
