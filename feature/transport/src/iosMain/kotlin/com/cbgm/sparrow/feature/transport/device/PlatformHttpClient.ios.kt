package com.cbgm.sparrow.feature.transport.device

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json

actual fun createPlatformHttpClient(json: Json): HttpClient =
    HttpClient(
        Darwin
    ) {
        expectSuccess = true

        install(WebSockets) {
            pingInterval = 20.seconds
        }

        install(ContentNegotiation) {
            json(json)
        }
    }
