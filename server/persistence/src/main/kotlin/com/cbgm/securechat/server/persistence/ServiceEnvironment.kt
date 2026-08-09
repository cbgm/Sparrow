package com.cbgm.securechat.server.persistence

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

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

class ControlPlaneEndpointPool(
    baseUrls: List<String>,
    private val failureCooldownMilliseconds: Long = DEFAULT_FAILURE_COOLDOWN_MILLISECONDS,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val lock = Any()
    private val endpoints = baseUrls.map(::normalize).distinct()
    private val failedUntil = mutableMapOf<String, Long>()
    private var activeEndpoint: String? = endpoints.firstOrNull()

    init {
        require(endpoints.isNotEmpty()) {
            "At least one control-plane URL is required"
        }
        require(failureCooldownMilliseconds > 0L) {
            "Control-plane failure cooldown must be positive"
        }
    }

    fun ordered(): List<String> =
        synchronized(lock) {
            val currentTime = now()
            val active = activeEndpoint
            endpoints.sortedWith(
                compareBy<String> { endpoint -> isCoolingDown(endpoint, currentTime) }
                    .thenBy { endpoint -> endpoint != active }
                    .thenBy { endpoint -> endpoints.indexOf(endpoint) }
            )
        }

    fun all(): List<String> = endpoints

    fun markAvailable(endpoint: String) {
        val normalized = normalize(endpoint)
        synchronized(lock) {
            failedUntil.remove(normalized)
            activeEndpoint = normalized
        }
    }

    fun markReachable(endpoint: String) {
        val normalized = normalize(endpoint)
        synchronized(lock) {
            failedUntil.remove(normalized)
        }
    }

    fun markUnavailable(endpoint: String) {
        val normalized = normalize(endpoint)
        synchronized(lock) {
            failedUntil[normalized] = now() + failureCooldownMilliseconds
            if (activeEndpoint == normalized) {
                activeEndpoint = null
            }
        }
    }

    private fun isCoolingDown(
        endpoint: String,
        currentTime: Long
    ): Boolean = (failedUntil[endpoint] ?: Long.MIN_VALUE) > currentTime

    companion object {
        fun fromEnvironment(
            legacyEnvironmentNames: List<String>,
            defaultUrl: String
        ): ControlPlaneEndpointPool =
            ControlPlaneEndpointPool(
                baseUrls =
                    controlPlaneUrlsFromEnvironment(
                        legacyEnvironmentNames = legacyEnvironmentNames,
                        defaultUrl = defaultUrl
                    )
            )

        private const val DEFAULT_FAILURE_COOLDOWN_MILLISECONDS = 15_000L
    }
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
