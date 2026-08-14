package com.cbgm.sparrow.feature.transport.gateway.codec

import kotlinx.serialization.json.Json

fun createGatewayJson(): Json =
    Json {
        classDiscriminator = "type"
        encodeDefaults = true
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = false
        prettyPrint = false
    }
