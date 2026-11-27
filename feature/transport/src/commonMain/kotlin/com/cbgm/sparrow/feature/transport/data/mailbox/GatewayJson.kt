package com.cbgm.sparrow.feature.transport.data.mailbox

import kotlinx.serialization.json.Json

/** Unknown fields are ignored, so a newer server version never breaks an older app. */
internal fun createGatewayJson(): Json =
    Json {
        classDiscriminator = "type"
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = false
        explicitNulls = false
        prettyPrint = false
    }
