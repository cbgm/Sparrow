package com.cbgm.securechat.feature.messaging.domain.relay

interface ContactRelayIdResolver {
    suspend fun resolve(contactId: String): Result<String>

    suspend fun resolveBootstrap(contactId: String): Result<String> = resolve(contactId)
}
