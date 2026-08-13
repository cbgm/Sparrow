package com.cbgm.securechat.feature.messaging.domain.relay

interface ContactByRelayIdResolver {
    suspend fun resolveContactId(relayId: String): Result<String?>

    suspend fun reconcileKnownContacts(): Result<Unit> = Result.success(Unit)
}
