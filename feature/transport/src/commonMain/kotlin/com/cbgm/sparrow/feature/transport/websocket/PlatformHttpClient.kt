package com.cbgm.sparrow.feature.transport.websocket

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

expect fun createPlatformHttpClient(json: Json): HttpClient
