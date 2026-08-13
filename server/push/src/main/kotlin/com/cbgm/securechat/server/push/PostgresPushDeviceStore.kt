package com.cbgm.securechat.server.push

internal class PostgresPushDeviceStore(
    private val database: PostgresPushDatabase,
    private val now: () -> Long = System::currentTimeMillis
) : PushDeviceStore {
    override suspend fun register(device: PushDevice) {
        database.withConnection { connection ->
            connection
                .prepareStatement(
                    """
                    INSERT INTO push_devices (
                        token,
                        relay_id,
                        platform,
                        updated_at_epoch_milliseconds
                    ) VALUES (?, ?, ?, ?)
                    ON CONFLICT (token) DO UPDATE SET
                        relay_id = EXCLUDED.relay_id,
                        platform = EXCLUDED.platform,
                        updated_at_epoch_milliseconds = EXCLUDED.updated_at_epoch_milliseconds
                    """.trimIndent()
                ).use { statement ->
                    var parameterIndex = 1
                    statement.setString(parameterIndex++, device.token)
                    statement.setString(parameterIndex++, device.routingId)
                    statement.setString(parameterIndex++, device.platform)
                    statement.setLong(parameterIndex, now())
                    statement.executeUpdate()
                }
        }
    }

    override suspend fun find(routingId: String): List<PushDevice> =
        database.withConnection { connection ->
            connection
                .prepareStatement(
                    """
                    SELECT relay_id, token, platform
                    FROM push_devices
                    WHERE relay_id = ?
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
            connection
                .prepareStatement("SELECT COUNT(*) FROM push_devices")
                .use { statement ->
                    statement.executeQuery().use { results ->
                        results.next()
                        results.getInt(1)
                    }
                }
        }
}
