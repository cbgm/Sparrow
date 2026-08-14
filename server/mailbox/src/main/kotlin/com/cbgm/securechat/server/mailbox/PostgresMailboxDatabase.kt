package com.cbgm.securechat.server.mailbox

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

internal data class PostgresMailboxDatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int
) {
    init {
        require(jdbcUrl.isNotBlank()) {
            "Mailbox database JDBC URL must not be blank"
        }
        require(maximumPoolSize > 0) {
            "Mailbox database maximum pool size must be positive"
        }
    }
}

internal class PostgresMailboxDatabase(
    config: PostgresMailboxDatabaseConfig
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
                poolName = "securechat-mailbox"
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
                MAILBOX_SCHEMA_STATEMENTS.forEach { sql ->
                    statement.execute(sql)
                }
            }
        }
    }

    private companion object {
        const val DATABASE_CONNECTION_TIMEOUT_MILLISECONDS = 10_000L
        const val DATABASE_VALIDATION_TIMEOUT_MILLISECONDS = 5_000L
        const val DATABASE_INITIALIZATION_TIMEOUT_MILLISECONDS = 60_000L

        val MAILBOX_SCHEMA_STATEMENTS =
            listOf(
                """
                CREATE TABLE IF NOT EXISTS mailboxes (
                    mailbox_id TEXT PRIMARY KEY,
                    owner_key_hash TEXT NOT NULL DEFAULT '',
                    send_capability_hash BYTEA NOT NULL,
                    retrieval_capability_hash BYTEA NOT NULL,
                    expires_at_epoch_milliseconds BIGINT NOT NULL
                )
                """.trimIndent(),
                """
                ALTER TABLE mailboxes
                ADD COLUMN IF NOT EXISTS owner_key_hash TEXT NOT NULL DEFAULT ''
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS mailboxes_expiry_idx
                ON mailboxes (expires_at_epoch_milliseconds)
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS mailboxes_owner_idx
                ON mailboxes (owner_key_hash)
                """.trimIndent(),
                """
                CREATE TABLE IF NOT EXISTS mailbox_envelopes (
                    mailbox_id TEXT NOT NULL REFERENCES mailboxes(mailbox_id) ON DELETE CASCADE,
                    envelope_id TEXT NOT NULL,
                    envelope_json TEXT NOT NULL,
                    payload_bytes BIGINT NOT NULL,
                    created_at_epoch_milliseconds BIGINT NOT NULL,
                    expires_at_epoch_milliseconds BIGINT NOT NULL,
                    PRIMARY KEY (mailbox_id, envelope_id)
                )
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS mailbox_envelopes_pending_idx
                ON mailbox_envelopes (mailbox_id, created_at_epoch_milliseconds, envelope_id)
                """.trimIndent(),
                """
                CREATE INDEX IF NOT EXISTS mailbox_envelopes_expiry_idx
                ON mailbox_envelopes (expires_at_epoch_milliseconds)
                """.trimIndent()
            )
    }
}

internal fun <T> Connection.inMailboxTransaction(block: () -> T): T {
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
