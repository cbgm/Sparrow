package com.cbgm.securechat.feature.messaging.application.relay

interface ContactRelayIdResolver {
    suspend fun resolve(contactId: String): Result<String>

    suspend fun resolveBootstrap(contactId: String): Result<String> = resolve(contactId)
}
