package com.cbgm.sparrow.feature.transport.data.controlplane

import com.cbgm.sparrow.feature.transport.domain.model.ControlPlaneEndpoint

internal fun String?.toUrlSet(): Set<String> =
    this?.lineSequence()?.mapNotNull { value ->
        runCatching { normalizeHttpUrl(value) }.getOrNull()?.takeIf { url ->
            CONTROL_PLANE_ORIGIN.matches(url)
        }
    }?.toSet() ?: emptySet()

internal fun combineEndpoints(
    manual: Set<String>,
    directory: Set<String>
): List<ControlPlaneEndpoint> =
    (manual.toList() + directory)
        .distinct()
        .map(::ControlPlaneEndpoint)

internal fun List<String>.normalizeUrls(): List<String> =
    map(String::trim)
        .filter(String::isNotBlank)
        .map(::normalizeHttpUrl)
        .distinct()

private val CONTROL_PLANE_ORIGIN = Regex("^https?://[A-Za-z0-9.-]+(?::[0-9]{1,5})?$")

internal fun normalizeHttpUrl(value: String): String {
    val trimmed = value.trim().trimEnd('/')
    require(trimmed.isNotBlank() && trimmed.none(Char::isWhitespace)) {
        "Control-plane URL is invalid"
    }
    val normalized =
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    require(CONTROL_PLANE_ORIGIN.matches(normalized)) {
        "Control Plane must be an HTTP(S) origin without path, query or invalid characters"
    }
    return ControlPlaneEndpoint(normalized).baseUrl
}
