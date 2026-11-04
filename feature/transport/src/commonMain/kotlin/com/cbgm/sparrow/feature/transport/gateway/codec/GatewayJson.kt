package com.cbgm.sparrow.feature.transport.gateway.codec

import kotlinx.serialization.json.Json

/** Unknown fields are ignored, so a newer server version never breaks an older app. */
fun createGatewayJson(): Json =
    Json {
        classDiscriminator = "type"
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = false
        explicitNulls = false
        prettyPrint = false
    }
