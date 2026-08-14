package com.cbgm.sparrow.server.mailbox

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

internal fun ApplicationCall.bearerCapability(): String? =
    request.headers[HttpHeaders.Authorization]
        ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
        ?.substringAfter(' ')
        ?.takeIf(String::isNotBlank)

private const val BEARER_PREFIX = "Bearer "
