package com.cbgm.sparrow.feature.messaging.application.routing

interface ContactRoutingIdResolver {
    suspend fun resolve(contactId: String): Result<String>

    suspend fun resolveBootstrap(contactId: String): Result<String> = resolve(contactId)
}
