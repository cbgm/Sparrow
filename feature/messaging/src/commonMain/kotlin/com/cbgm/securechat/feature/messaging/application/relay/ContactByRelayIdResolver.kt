package com.cbgm.securechat.feature.messaging.application.relay

interface ContactByRelayIdResolver {
    suspend fun resolveContactId(relayId: String): Result<String?>

    suspend fun reconcileKnownContacts(): Result<Unit> = Result.success(Unit)
}
