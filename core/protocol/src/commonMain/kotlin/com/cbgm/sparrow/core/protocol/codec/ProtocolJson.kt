package com.cbgm.sparrow.core.protocol.codec

import kotlinx.serialization.json.Json

fun createProtocolJson(): Json =
    Json {
        /**
         * Produces:
         *
         * {
         *   "packetType": "chat_message",
         *   "version": 1,
         *   ...
         * }
         */
        classDiscriminator = "packetType"

        encodeDefaults = true

        ignoreUnknownKeys = false

        isLenient = false

        explicitNulls = false

        prettyPrint = false
    }
