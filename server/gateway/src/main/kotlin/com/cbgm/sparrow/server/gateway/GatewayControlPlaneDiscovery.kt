package com.cbgm.sparrow.server.gateway

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

internal fun Route.installControlPlaneDiscoveryRoute(config: GatewayConfig) {
    val discovery = GatewayControlPlaneDiscovery(config.advertisedControlPlaneUrls)
    get("/v1/control-planes") {
        // The publication is read on every request: no Gateway restart is needed.
        call.respond(GatewayControlPlaneDirectory(discovery.readUrls()))
    }
}

/**
 * The directory worker verifies signed snapshots before publishing the local file.
 * Its URLs are discovery hints, never permission to replace a trusted signing key.
 */
private class GatewayControlPlaneDiscovery(
    private val configuredUrls: List<String>
) {
    private val localUrl = System.getenv("LOCAL_CONTROL_PLANE_DOMAIN")
        ?.trim()
        ?.takeIf(DOMAIN::matches)
        ?.let { "https://$it" }

    private val publicationPath = System.getenv("DIRECTORY_DISCOVERY_PATH")
        ?.takeIf(String::isNotBlank)
        ?.let(Path::of)

    private val manualUrls = System.getenv("MANUAL_CONTROL_PLANE_URLS")
        ?.split(',', ';')
        ?.map(String::trim)
        ?.filter(::isSafeManualPlaneOrigin)
        .orEmpty()

    fun readUrls(): List<String> {
        val publication = publicationPath?.let(::readPublishedUrls) ?: return configuredUrls
        // Node-only has no paired local Control Plane. Still consume its own
        // verified directory worker's publication, retaining explicit manual
        // planes separately. A signed *empty* publication must not resurrect
        // previously discovered, now-revoked entries from startup config.
        return (listOfNotNull(localUrl) + manualUrls + publication).distinct()
    }

    private fun readPublishedUrls(path: Path): List<String>? = try {
        if (!Files.isRegularFile(path) || Files.size(path) > MAX_PUBLICATION_BYTES) {
            null
        } else {
            parsePublication(Files.readString(path))
        }
    } catch (_: Exception) {
        // No valid publication: keep the existing configured fallback, so a
        // directory outage never erases the already selected Control Plane.
        null
    }

    private fun parsePublication(content: String): List<String>? {
        val root = Json.parseToJsonElement(content).jsonObject
        if (root.keys != PUBLICATION_FIELDS) return null

        val urls = root.getValue("controlPlanes").jsonArray.map { it.jsonPrimitive.content }
        if (urls.size > MAX_CONTROL_PLANES || urls.size != urls.distinct().size ||
            urls.any { !isSafePublicPlaneOrigin(it) }
        ) {
            return null
        }

        val entryUrls = root.getValue("entries").jsonArray.map { entry ->
            entry.jsonObject.getValue("baseUrl").jsonPrimitive.content
        }
        return if (urls == entryUrls) urls else null
    }
}

private fun isSafeManualPlaneOrigin(value: String): Boolean {
    if (isSafePublicPlaneOrigin(value)) return true
    return try {
        val uri = URI(value)
        uri.scheme == "http" && uri.host != null && uri.rawUserInfo == null &&
            uri.port in 1..65535 && uri.rawQuery == null && uri.rawFragment == null &&
            (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") && DOMAIN.matches(uri.host)
    } catch (_: IllegalArgumentException) {
        false
    }
}

private fun isSafePublicPlaneOrigin(value: String): Boolean {
    if (!value.startsWith("https://") || value.length > 253) return false
    return try {
        val uri = URI(value)
        uri.host != null && uri.rawUserInfo == null && uri.port in listOf(-1, 443) &&
            uri.rawQuery == null && uri.rawFragment == null &&
            (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") &&
            DOMAIN.matches(uri.host)
    } catch (_: IllegalArgumentException) {
        false
    }
}

@Serializable
private data class GatewayControlPlaneDirectory(
    val controlPlanes: List<String>
)

private val DOMAIN = Regex("[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?")
private val PUBLICATION_FIELDS = setOf("controlPlanes", "entries")
private const val MAX_PUBLICATION_BYTES = 512 * 1024L
private const val MAX_CONTROL_PLANES = 2_048
