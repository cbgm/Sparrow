package com.cbgm.securechat.server.observability

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.system.exitProcess

private const val HTTP_SUCCESS_MINIMUM = 200
private const val HTTP_SUCCESS_MAXIMUM = 299

object ReadinessProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        val endpoint = args.singleOrNull() ?: exitProcess(1)
        val statusCode =
            runCatching {
                val request =
                    HttpRequest
                        .newBuilder(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build()
                HttpClient
                    .newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.discarding())
                    .statusCode()
            }.getOrDefault(0)

        exitProcess(if (statusCode in HTTP_SUCCESS_MINIMUM..HTTP_SUCCESS_MAXIMUM) 0 else 1)
    }
}
