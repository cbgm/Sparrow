package com.cbgm.securechat.server.push

internal fun createPostgresPushStores(config: PushConfig): PushStores {
    val database =
        PostgresPushDatabase(
            config =
                PostgresPushDatabaseConfig(
                    jdbcUrl =
                        requireNotNull(config.databaseUrl) {
                            "Push database URL is required"
                        },
                    username = config.databaseUser,
                    password = config.databasePassword,
                    maximumPoolSize = config.databaseMaximumPoolSize
                )
        )

    return PushStores(
        devices = PostgresPushDeviceStore(database),
        pendingEnvelopes =
            PostgresPendingEnvelopeStore(
                database = database,
                maximumEnvelopes = config.maximumEnvelopes,
                retentionMilliseconds = config.envelopeRetentionMilliseconds
            ),
        wakeUps =
            PostgresWakeUpStore(
                database = database,
                lifetimeMilliseconds = config.wakeUpLifetimeMilliseconds
            ),
        persistenceMode = "postgresql",
        closeAction = database::close
    )
}
