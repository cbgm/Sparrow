package com.cbgm.sparrow.feature.transport.data.http

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

internal expect fun createPlatformHttpClient(json: Json): HttpClient
