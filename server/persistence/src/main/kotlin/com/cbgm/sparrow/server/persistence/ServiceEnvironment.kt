package com.cbgm.sparrow.server.persistence

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object ServiceEnvironment {
    fun string(
        name: String,
        defaultValue: String
    ): String = System.getenv(name)?.takeIf(String::isNotBlank) ?: defaultValue

    fun int(
        name: String,
        defaultValue: Int
    ): Int = System.getenv(name)?.toIntOrNull() ?: defaultValue

    fun long(
        name: String,
        defaultValue: Long
    ): Long = System.getenv(name)?.toLongOrNull() ?: defaultValue

    fun secret(name: String): String? =
        resolveSecret(
            name = name,
            environment = System::getenv,
            readFile = { path -> Files.readString(Path.of(path)) }
        )

    internal fun resolveSecret(
        name: String,
        environment: (String) -> String?,
        readFile: (String) -> String
    ): String? {
        val fileVariable = "${name}_FILE"
        val secretFile = environment(fileVariable)?.takeIf(String::isNotBlank)
        if (secretFile != null) {
            return readFile(secretFile)
                .trimEnd('\r', '\n')
                .takeIf(String::isNotBlank)
                ?: error("Secret file configured by $fileVariable is empty")
        }
        return environment(name)?.takeIf(String::isNotBlank)
    }
}

/**
 * Static, explicitly configured endpoints plus the read-only publication from
 * the isolated, signature-verifying directory-sync worker. The worker is the
 * trust boundary: the file MUST NOT be writable by federation or mailbox and
 * must never be populated from an unauthenticated HTTP/JSON response.
 */
class ControlPlaneEndpointPool(
    baseUrls: List<String>,
    private val failureCooldownMilliseconds: Long = DEFAULT_FAILURE_COOLDOWN_MILLISECONDS,
    private val now: () -> Long = System::currentTimeMillis,
    private val discoveryFile: Path? = null
) {
    private val lock = Any()
    private val configuredEndpoints = baseUrls.map(::normalize).distinct()
    private val failedUntil = mutableMapOf<String, Long>()
    private var activeEndpoint: String? = configuredEndpoints.firstOrNull()
    private var lastDiscoveryReadAt = Long.MIN_VALUE
    private var cachedDiscoveryEntries: Map<String, ByteArray> = emptyMap()

    init {
        require(configuredEndpoints.isNotEmpty()) { "At least one control-plane URL is required" }
        require(failureCooldownMilliseconds > 0L) { "Control-plane failure cooldown must be positive" }
    }

    /** Read on use: additions and authenticated removals need no JVM restart. */
    fun all(): List<String> = (configuredEndpoints + readDiscoveredEntries().keys).distinct()

    /**
     * The independently verified root key for a dynamically discovered plane.
     * Explicitly configured planes retain their existing trust semantics and
     * must NOT have their identity silently changed by a directory entry.
     */
    fun expectedDirectoryRootKey(baseUrl: String): ByteArray? =
        if (baseUrl in configuredEndpoints) null else readDiscoveredEntries()[baseUrl]?.copyOf()

    fun ordered(): List<String> = synchronized(lock) {
        val endpoints = all()
        val currentTime = now()
        val active = activeEndpoint
        failedUntil.keys.retainAll(endpoints.toSet())
        if (active !in endpoints) activeEndpoint = null
        endpoints.sortedWith(
            compareBy<String> { endpoint -> isCoolingDown(endpoint, currentTime) }
                .thenBy { endpoint -> endpoint != active }
                .thenBy { endpoint -> endpoints.indexOf(endpoint) }
        )
    }

    fun availableEndpoints(): List<String> = synchronized(lock) {
        val currentTime = now()
        all().filterNot { endpoint -> isCoolingDown(endpoint, currentTime) }
    }

    fun markAvailable(endpoint: String) {
        val normalized = normalize(endpoint)
        synchronized(lock) {
            // An in-flight request to a revoked URL must not make it active.
            if (normalized !in all()) return
            failedUntil.remove(normalized)
            activeEndpoint = normalized
        }
    }

    fun markReachable(endpoint: String) {
        val normalized = normalize(endpoint)
        synchronized(lock) { failedUntil.remove(normalized) }
    }

    fun markUnavailable(endpoint: String) {
        val normalized = normalize(endpoint)
        synchronized(lock) {
            failedUntil[normalized] = now() + failureCooldownMilliseconds
            if (activeEndpoint == normalized) activeEndpoint = null
        }
    }

    private fun readDiscoveredEntries(): Map<String, ByteArray> = synchronized(lock) {
        val file = discoveryFile ?: return@synchronized emptyMap()
        // Descriptors may be routed many times per second. Re-parse at most
        // once a second while still noticing additions/revocations live.
        val currentTime = now()
        if (currentTime >= lastDiscoveryReadAt &&
            currentTime - lastDiscoveryReadAt < DIRECTORY_READ_INTERVAL_MILLISECONDS
        ) {
            return@synchronized cachedDiscoveryEntries
        }
        lastDiscoveryReadAt = currentTime
        val parsed = try {
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_DISCOVERY_BYTES) throw IllegalArgumentException("Invalid directory publication")
            val root = Json.parseToJsonElement(Files.readString(file)).jsonObject
            if (root.keys != setOf("controlPlanes", "entries")) throw IllegalArgumentException("Invalid directory publication")
            val urls = root.getValue("controlPlanes").jsonArray
                .map { it.jsonPrimitive.content }
            val entries = root.getValue("entries").jsonArray
            if (entries.size > MAX_DIRECTORY_ENTRIES || entries.size != urls.size ||
                urls.size != urls.distinct().size
            ) {
                throw IllegalArgumentException("Invalid directory publication")
            }
            val verified = linkedMapOf<String, ByteArray>()
            for (item in entries) {
                val entry = item.jsonObject
                if (entry.keys != setOf("baseUrl", "controlPlaneId", "publicKey")) throw IllegalArgumentException("Invalid directory publication")
                val url = entry.getValue("baseUrl").jsonPrimitive.content
                val id = entry.getValue("controlPlaneId").jsonPrimitive.content
                val encodedKey = entry.getValue("publicKey").jsonPrimitive.content
                if (!isSafeDiscoveredOrigin(url) || !id.matches(Regex("[a-f0-9]{64}"))) throw IllegalArgumentException("Invalid directory publication")
                val key = Base64.getUrlDecoder().decode(encodedKey)
                if (key.size != ED25519_SPKI_BYTES ||
                    Base64.getUrlEncoder().withoutPadding().encodeToString(key) != encodedKey ||
                    MessageDigest.getInstance("SHA-256").digest(key)
                        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) } != id
                ) {
                    throw IllegalArgumentException("Invalid directory publication")
                }
                if (!KeyFactory.getInstance("Ed25519")
                        .generatePublic(X509EncodedKeySpec(key)).encoded.contentEquals(key)
                ) {
                    throw IllegalArgumentException("Invalid directory publication")
                }
                if (verified.put(url, key) != null) throw IllegalArgumentException("Invalid directory publication")
            }
            if (verified.keys.toList() != urls) throw IllegalArgumentException("Invalid directory publication")
            verified
        } catch (_: Exception) {
            // A missing or malformed publication is not an authority to
            // enroll another signing identity or restore a revoked endpoint.
            emptyMap()
        }
        cachedDiscoveryEntries = parsed
        parsed
    }

    private fun isCoolingDown(endpoint: String, currentTime: Long): Boolean =
        (failedUntil[endpoint] ?: Long.MIN_VALUE) > currentTime

    companion object {
        fun fromEnvironment(legacyEnvironmentNames: List<String>, defaultUrl: String): ControlPlaneEndpointPool =
            ControlPlaneEndpointPool(
                baseUrls = controlPlaneUrlsFromEnvironment(legacyEnvironmentNames, defaultUrl),
                discoveryFile = System.getenv("DIRECTORY_DISCOVERY_PATH")
                    ?.takeIf(String::isNotBlank)
                    ?.let { Path.of(it) }
            )

        private const val DEFAULT_FAILURE_COOLDOWN_MILLISECONDS = 15_000L
        private const val MAX_DISCOVERY_BYTES = 512L * 1024L
        private const val MAX_DIRECTORY_ENTRIES = 2048
        private const val ED25519_SPKI_BYTES = 44
        private const val DIRECTORY_READ_INTERVAL_MILLISECONDS = 1_000L
    }
}

/** Only signed-worker public HTTPS origins may enter dynamic routing. */
private fun isSafeDiscoveredOrigin(value: String): Boolean {
    if (value.length > 253 || !value.startsWith("https://")) return false
    val uri = runCatching { URI(value) }.getOrNull() ?: return false
    return uri.host != null && uri.rawUserInfo == null && uri.port in listOf(-1, 443) &&
        uri.rawQuery == null && uri.rawFragment == null &&
        (uri.rawPath.isNullOrEmpty() || uri.rawPath == "/") &&
        uri.host.matches(Regex("[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?")) &&
        value == "https://${uri.host.lowercase()}" // canonical origins only
}

fun controlPlaneUrlsFromEnvironment(
    legacyEnvironmentNames: List<String>,
    defaultUrl: String
): List<String> {
    val configured = System.getenv("CONTROL_PLANE_URLS")?.takeIf(String::isNotBlank)
    if (configured != null) {
        return configured
            .split(',', ';')
            .map(String::trim)
            .filter(String::isNotBlank)
            .map(::normalize)
            .distinct()
            .also { urls ->
                require(urls.isNotEmpty()) {
                    "CONTROL_PLANE_URLS contains no usable URLs"
                }
            }
    }

    val legacyUrl =
        legacyEnvironmentNames
            .asSequence()
            .mapNotNull { name -> System.getenv(name)?.takeIf(String::isNotBlank) }
            .firstOrNull()
            ?: defaultUrl
    return listOf(normalize(legacyUrl))
}

private fun normalize(value: String): String {
    val normalized = value.trim().trimEnd('/')
    val uri = runCatching { URI(normalized) }.getOrNull()
    require(
        uri != null &&
            uri.isAbsolute &&
            uri.host != null &&
            uri.scheme in setOf("http", "https")
    ) {
        "Invalid control-plane URL: $value"
    }
    return normalized
}
