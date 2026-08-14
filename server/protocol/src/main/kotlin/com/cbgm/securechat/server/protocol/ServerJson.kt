package com.cbgm.securechat.server.protocol

import kotlinx.serialization.json.Json

val serverJson: Json =
    Json {
        classDiscriminator = "type"
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
    }
