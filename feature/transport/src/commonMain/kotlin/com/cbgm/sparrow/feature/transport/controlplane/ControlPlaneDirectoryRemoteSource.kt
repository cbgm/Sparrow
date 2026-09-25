package com.cbgm.sparrow.feature.transport.controlplane

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.milliseconds

/** Network and wire-format parsing only; does not mutate configuration, cache, or trust. */
internal class ControlPlaneDirectoryRemoteSource(
    private val httpClient: HttpClient,
    private val json: Json
) {
    suspend fun fetchSigned(url: String): SignedDirectoryDownload {
        val baseUrl = normalizeDirectoryOrigin(url)
        val bootstrap = json.parseToJsonElement(
            getText("$baseUrl/.well-known/sparrow-directory", MAX_BOOTSTRAP_BYTES)
        ).jsonObject
        require(
            bootstrap.keys == BOOTSTRAP_KEYS &&
                bootstrap.getValue("protocol").jsonPrimitive.content == BOOTSTRAP_PROTOCOL &&
                bootstrap.getValue("controlPlanesEndpoint").jsonPrimitive.content == "/v1/control-planes"
        ) { "Unsupported directory bootstrap document" }

        return SignedDirectoryDownload(
            baseUrl = baseUrl,
            publicKey = bootstrap.getValue("publicKey").jsonPrimitive.content,
            keyId = bootstrap.getValue("keyId").jsonPrimitive.content,
            envelope = getText("$baseUrl/v1/control-planes", MAX_DOCUMENT_BYTES)
        )
    }

    suspend fun fetchJsonAddresses(url: String): List<String> {
        val address = url.trim()
        val origin = Url(address)
        require(origin.protocol.name == "http" || origin.protocol.name == "https") {
            "JSON list URL must use HTTP or HTTPS"
        }
        require(origin.host.isNotBlank() && !address.contains('@') && !address.contains('#')) {
            "JSON list URL contains unsupported components"
        }
        val body = withTimeout(REQUEST_TIMEOUT.milliseconds) {
            val response = httpClient.get(address)
            check(response.status.isSuccess()) { "JSON list returned HTTP ${response.status.value}" }
            response.bodyAsText()
        }
        require(body.encodeToByteArray().size <= MAX_JSON_LIST_BYTES) { "JSON list is too large" }
        val entries = json.parseToJsonElement(body).jsonObject.getValue("controlPlanes").jsonArray
        require(entries.isNotEmpty() && entries.size <= MAX_JSON_ENTRIES) { "Invalid JSON list size" }
        return entries.map { item ->
            item.jsonPrimitive.content.trim().trimEnd('/').also { candidate ->
                require(PLANE_ORIGIN.matches(candidate)) {
                    "JSON list includes a value that is not a Control Plane origin"
                }
            }
        }.distinct()
    }

    private suspend fun getText(url: String, maxBytes: Int): String =
        withTimeout(REQUEST_TIMEOUT.milliseconds) {
            val response = httpClient.get(url)
            check(response.status.isSuccess() && response.request.url.toString() == url) {
                "Directory response must be retrieved directly from the configured HTTPS origin"
            }
            response.bodyAsText().also { body ->
                require(body.encodeToByteArray().size <= maxBytes) { "Directory response is too large" }
            }
        }

    private fun normalizeDirectoryOrigin(url: String): String {
        val normalized = url.trim().trimEnd('/')
        require(normalized.startsWith("https://") && normalized.length <= 253) {
            "Directory must have a HTTPS base URL"
        }
        val host = normalized.removePrefix("https://")
        require(
            host.isNotEmpty() && host.count { it == '.' } > 0 &&
                host.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' || it == '.' } &&
                host.split('.').all { label ->
                    label.isNotEmpty() && label.length <= 63 &&
                        label.first().isLetterOrDigit() && label.last().isLetterOrDigit()
                }
        ) { "Directory URL must be an HTTPS hostname origin without path, credentials or query" }
        return "https://${host.lowercase()}"
    }

    private companion object {
        const val REQUEST_TIMEOUT = 8_000L
        const val MAX_BOOTSTRAP_BYTES = 4 * 1024
        const val MAX_DOCUMENT_BYTES = 512 * 1024
        const val MAX_JSON_LIST_BYTES = 64 * 1024
        const val MAX_JSON_ENTRIES = 64
        const val BOOTSTRAP_PROTOCOL = "sparrow-directory-bootstrap-v1"
        val BOOTSTRAP_KEYS = setOf("protocol", "keyId", "publicKey", "controlPlanesEndpoint")
        val PLANE_ORIGIN = Regex("^https?://[A-Za-z0-9.-]+(?::[0-9]{1,5})?$")
    }
}

internal data class SignedDirectoryDownload(
    val baseUrl: String,
    val publicKey: String,
    val keyId: String,
    val envelope: String
)
