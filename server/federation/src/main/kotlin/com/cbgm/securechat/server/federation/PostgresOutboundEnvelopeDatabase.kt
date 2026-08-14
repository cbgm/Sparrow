package com.cbgm.securechat.server.federation

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

internal data class PostgresOutboundEnvelopeDatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int
) {
    init {
        require(jdbcUrl.isNotBlank()) {
            "Federation database JDBC URL must not be blank"
        }
        require(maximumPoolSize > 0) {
            "Federation database maximum pool size must be positive"
        }
    }
}

internal class PostgresOutboundEnvelopeDatabase(
    config: PostgresOutboundEnvelopeDatabaseConfig
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
                poolName = "securechat-federation"
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
                FEDERATION_SCHEMA_STATEMENTS.forEach { sql ->
                    statement.execute(sql)
                }
            }
        }
    }

    private companion object {
        const val DATABASE_CONNECTION_TIMEOUT_MILLISECONDS = 10_000L
        const val DATABASE_VALIDATION_TIMEOUT_MILLISECONDS = 5_000L
        const val DATABASE_INITIALIZATION_TIMEOUT_MILLISECONDS = 60_000L

        val FEDERATION_SCHEMA_STATEMENTS =
            listOf(
                """
                CREATE TABLE IF NOT EXISTS federation_outbound_envelopes (
                    envelope_id TEXT PRIMARY KEY,
                    envelope_json TEXT NOT NULL,
                    state TEXT NOT NULL,
                    attempts INTEGER NOT NULL,
                    next_attempt_at_epoch_milliseconds BIGINT NOT NULL,
                    expires_at_epoch_milliseconds BIGINT NOT NULL,
                    updated_at_epoch_milliseconds BIGINT NOT NULL
                )
                """.trimIndent(),
                """
                ALTER TABLE federation_outbound_envelopes
                ADD COLUMN IF NOT EXISTS routing_version INTEGER NOT NULL DEFAULT 0
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS federation_outbound_pending_idx
                ON federation_outbound_envelopes (
                    state,
                    next_attempt_at_epoch_milliseconds,
                    envelope_id
                )
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS federation_outbound_expiry_idx
                ON federation_outbound_envelopes (expires_at_epoch_milliseconds)
                """.trimIndent()
            )
    }
}
