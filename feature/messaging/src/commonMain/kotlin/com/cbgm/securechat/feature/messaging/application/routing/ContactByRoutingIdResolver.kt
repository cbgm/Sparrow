package com.cbgm.securechat.feature.messaging.application.routing

interface ContactByRoutingIdResolver {
    suspend fun resolveContactId(routingId: String): Result<String?>

    suspend fun reconcileKnownContacts(): Result<Unit> = Result.success(Unit)
}
