package com.cbgm.securechat.server.push

import java.security.SecureRandom
import java.sql.Connection
import java.util.Base64

internal class PostgresWakeUpStore(
    private val database: PostgresPushDatabase,
    private val lifetimeMilliseconds: Long,
    private val now: () -> Long = System::currentTimeMillis
) : WakeUpStore {
    private val random = SecureRandom()

    init {
        require(lifetimeMilliseconds > 0L) {
            "Wake-up lifetime must be positive"
        }
    }

    override suspend fun create(recipientId: String): String =
        database.withConnection { connection ->
            purgeExpired(connection)

            val wakeUpId = createWakeUpId()

            connection
                .prepareStatement(
                    """
                    INSERT INTO push_wake_ups (
                        wake_up_id,
                        recipient_id,
                        expires_at_epoch_milliseconds
                    ) VALUES (?, ?, ?)
                    """.trimIndent()
                ).use { statement ->
                    var parameterIndex = 1
                    statement.setString(parameterIndex++, wakeUpId)
                    statement.setString(parameterIndex++, recipientId)
                    statement.setLong(parameterIndex, now() + lifetimeMilliseconds)
                    statement.executeUpdate()
                }

            wakeUpId
        }

    override suspend fun resolve(wakeUpId: String?): String? {
        if (wakeUpId.isNullOrBlank()) {
            return null
        }

        return database.withConnection { connection ->
            purgeExpired(connection)

            connection
                .prepareStatement(
                    """
                    SELECT recipient_id
                    FROM push_wake_ups
                    WHERE wake_up_id = ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, wakeUpId)
                    statement.executeQuery().use { results ->
                        if (results.next()) {
                            results.getString("recipient_id")
                        } else {
                            null
                        }
                    }
                }
        }
    }

    private fun purgeExpired(connection: Connection) {
        connection
            .prepareStatement(
                "DELETE FROM push_wake_ups WHERE expires_at_epoch_milliseconds <= ?"
            ).use { statement ->
                statement.setLong(1, now())
                statement.executeUpdate()
            }
    }

    private fun createWakeUpId(): String {
        val bytes = ByteArray(WAKE_UP_ID_BYTE_COUNT)
        random.nextBytes(bytes)

        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)
    }

    private companion object {
        const val WAKE_UP_ID_BYTE_COUNT = 32
    }
}
