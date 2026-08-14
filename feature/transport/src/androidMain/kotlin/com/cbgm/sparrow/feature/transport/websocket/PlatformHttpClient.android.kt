package com.cbgm.sparrow.feature.transport.websocket

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

actual fun createPlatformHttpClient(json: Json): HttpClient =
    HttpClient(
        OkHttp
    ) {
        expectSuccess = true

        install(WebSockets)

        install(ContentNegotiation) {
            json(json)
        }

        engine {
            config {
                retryOnConnectionFailure(true)

                pingInterval(
                    20L,
                    TimeUnit.SECONDS
                )
            }
        }
    }
