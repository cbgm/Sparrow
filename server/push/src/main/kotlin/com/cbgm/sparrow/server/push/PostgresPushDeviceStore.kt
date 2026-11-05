package com.cbgm.sparrow.server.push

import java.sql.Connection

/**
 * Each row expires [lifetimeMilliseconds] after its last registration. Expired rows are deleted before
 * every read or write, like expired envelopes and wake-ups, so they are never used for a wake-up.
 */
internal class PostgresPushDeviceStore(
    private val database: PostgresPushDatabase,
    private val lifetimeMilliseconds: Long,
    private val now: () -> Long = System::currentTimeMillis
) : PushDeviceStore {
    init {
        require(lifetimeMilliseconds > 0L) {
            "Push device lifetime must be positive"
        }
    }

    override suspend fun register(device: PushDevice) {
        database.withConnection { connection ->
            purgeExpired(connection)

            val currentTime = now()
            connection
                .prepareStatement(
                    """
                    INSERT INTO push_devices (
                        token,
                        routing_id,
                        platform,
                        updated_at_epoch_milliseconds,
                        expires_at_epoch_milliseconds
                    ) VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (token) DO UPDATE SET
                        routing_id = EXCLUDED.routing_id,
                        platform = EXCLUDED.platform,
                        updated_at_epoch_milliseconds = EXCLUDED.updated_at_epoch_milliseconds,
                        expires_at_epoch_milliseconds = EXCLUDED.expires_at_epoch_milliseconds
                    """.trimIndent()
                ).use { statement ->
                    var parameterIndex = 1
                    statement.setString(parameterIndex++, device.token)
                    statement.setString(parameterIndex++, device.routingId)
                    statement.setString(parameterIndex++, device.platform)
                    statement.setLong(parameterIndex++, currentTime)
                    statement.setLong(parameterIndex, currentTime + lifetimeMilliseconds)
                    statement.executeUpdate()
                }
        }
    }

    override suspend fun find(routingId: String): List<PushDevice> =
        database.withConnection { connection ->
            purgeExpired(connection)

            connection
                .prepareStatement(
                    """
                    SELECT routing_id, token, platform
                    FROM push_devices
                    WHERE routing_id = ?
                    ORDER BY token
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, routingId)
                    statement.executeQuery().use { results ->
                        results.readPushDevices()
                    }
                }
        }

    override suspend fun removeToken(token: String) {
        database.withConnection { connection ->
            connection
                .prepareStatement(
                    "DELETE FROM push_devices WHERE token = ?"
                ).use { statement ->
                    statement.setString(1, token)
                    statement.executeUpdate()
                }
        }
    }

    override suspend fun count(): Int =
        database.withConnection { connection ->
            purgeExpired(connection)

            connection
                .prepareStatement("SELECT COUNT(*) FROM push_devices")
                .use { statement ->
                    statement.executeQuery().use { results ->
                        results.next()
                        results.getInt(1)
                    }
                }
        }

    private fun purgeExpired(connection: Connection) {
        connection
            .prepareStatement(
                "DELETE FROM push_devices WHERE expires_at_epoch_milliseconds <= ?"
            ).use { statement ->
                statement.setLong(1, now())
                statement.executeUpdate()
            }
    }
}
