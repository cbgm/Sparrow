package com.cbgm.sparrow.feature.transport.controlplane

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Owns the pinned directory identity, anti-rollback state and authenticated offline snapshot. */
internal class VerifiedControlPlaneDirectoryState(
    private val configuration: ControlPlaneConfiguration,
    private val verifier: SignedControlPlaneDirectoryVerifier,
    private val dataStore: SparrowDataStore,
    private val json: Json,
    private val now: () -> Long = SystemClock::nowEpochMilliseconds
) {
    private val logger = SparrowLog.withTag("ControlPlaneDirectory")
    private var loaded = false
    private var lastVerified: DirectoryPayload? = null
    private var trustedPublicKey: String? = null
    private var stalePlanesByBaseUrl: Map<String, StaleDirectoryPlane> = emptyMap()

    val hasCachedSnapshot: Boolean get() = lastVerified != null

    suspend fun restore() {
        if (loaded) return
        val storedKey = dataStore.getString(KEY_TRUSTED_DIRECTORY_PUBLIC_KEY)
        val storedEnvelope = dataStore.getString(KEY_SIGNED_ENVELOPE)
        val storedStalePlanes = dataStore.getString(KEY_STALE_DISCOVERED_PLANES)
        loaded = true
        // Never replace an existing pin merely because its cached envelope is corrupt.
        trustedPublicKey = storedKey?.takeIf(String::isNotBlank)
        stalePlanesByBaseUrl = decodeStalePlanes(storedStalePlanes)
        if (trustedPublicKey == null || storedEnvelope == null) return
        try {
            val payload = verifier.verify(storedEnvelope, trustedPublicKey!!, allowExpired = true)
            applyRestored(payload)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            logger.warn { "Ignoring invalid cached Control Plane directory: ${error.message}" }
        }
    }

    suspend fun accept(download: SignedDirectoryDownload, saveUrl: Boolean): Int {
        require(trustedPublicKey == null || trustedPublicKey == download.publicKey) {
            "Directory signing identity changed; refusing silent replacement"
        }
        val payload = verifier.verify(download.envelope, download.publicKey)
        val signedKeyId = json.parseToJsonElement(download.envelope).jsonObject
            .getValue("keyId").jsonPrimitive.content
        require(signedKeyId == download.keyId) {
            "Directory bootstrap key ID differs from signed snapshot"
        }
        checkNoRollback(payload)
        require(saveUrl || configuration.directoryUrl.value == download.baseUrl) {
            "Configured signed directory changed during refresh"
        }

        val nextStalePlanes = nextStalePlanes(payload)
        val mergedRoots = mergedRoots(payload, nextStalePlanes)
        configuration.replaceVerifiedDirectory(
            rootsByBaseUrl = mergedRoots,
            staleBaseUrls = nextStalePlanes.keys
        ).getOrThrow()

        // Save the trusted key only after validating the complete signed snapshot.
        dataStore.edit {
            putString(KEY_TRUSTED_DIRECTORY_PUBLIC_KEY, download.publicKey)
            putString(KEY_SIGNED_ENVELOPE, download.envelope)
            if (nextStalePlanes.isEmpty()) {
                removeString(KEY_STALE_DISCOVERED_PLANES)
            } else {
                putString(
                    KEY_STALE_DISCOVERED_PLANES,
                    json.encodeToString(nextStalePlanes.values.toList())
                )
            }
        }
        trustedPublicKey = download.publicKey
        stalePlanesByBaseUrl = nextStalePlanes
        lastVerified = payload
        if (saveUrl) configuration.setDirectoryUrl(download.baseUrl).getOrThrow()
        return configuration.directoryBaseUrls.value.size
    }

    suspend fun forgetAfterRemoval() {
        require(configuration.directoryUrl.value == null) {
            "Remove the configured directory before resetting its signing key"
        }
        dataStore.edit {
            removeString(KEY_TRUSTED_DIRECTORY_PUBLIC_KEY)
            removeString(KEY_SIGNED_ENVELOPE)
            removeString(KEY_STALE_DISCOVERED_PLANES)
        }
        // Called under the synchronizer mutex: a pending refresh cannot restore removed entries.
        configuration.replaceVerifiedDirectory(emptyMap()).getOrThrow()
        lastVerified = null
        trustedPublicKey = null
        stalePlanesByBaseUrl = emptyMap()
        loaded = true
    }

    private suspend fun applyRestored(payload: DirectoryPayload) {
        val retainedStalePlanes = pruneStalePlanes(payload, stalePlanesByBaseUrl)
        configuration.replaceVerifiedDirectory(
            rootsByBaseUrl = mergedRoots(payload, retainedStalePlanes),
            staleBaseUrls = retainedStalePlanes.keys
        ).getOrThrow()
        if (retainedStalePlanes != stalePlanesByBaseUrl) {
            persistStalePlanes(retainedStalePlanes)
        }
        stalePlanesByBaseUrl = retainedStalePlanes
        lastVerified = payload
    }

    private fun nextStalePlanes(payload: DirectoryPayload): Map<String, StaleDirectoryPlane> {
        val currentUrls = payload.controlPlanes.mapTo(mutableSetOf(), DirectoryPlane::baseUrl)
        val revokedIds = payload.revokedControlPlaneIds.toSet()
        val timestamp = now()
        val next = pruneStalePlanes(payload, stalePlanesByBaseUrl).toMutableMap()

        // Only entries previously authenticated by the signed Directory can
        // enter the grace period. Manual Control Planes live in a different
        // store and are deliberately not considered here.
        lastVerified?.controlPlanes.orEmpty().forEach { previous ->
            if (
                previous.baseUrl !in currentUrls &&
                previous.controlPlaneId !in revokedIds &&
                previous.baseUrl !in next
            ) {
                next[previous.baseUrl] =
                    StaleDirectoryPlane(
                        baseUrl = previous.baseUrl,
                        controlPlaneId = previous.controlPlaneId,
                        staleSinceEpochMilliseconds = timestamp
                    )
            }
        }
        return next
    }

    private fun pruneStalePlanes(
        payload: DirectoryPayload,
        candidates: Map<String, StaleDirectoryPlane>
    ): Map<String, StaleDirectoryPlane> {
        val currentUrls = payload.controlPlanes.mapTo(mutableSetOf(), DirectoryPlane::baseUrl)
        val revokedIds = payload.revokedControlPlaneIds.toSet()
        val timestamp = now()
        return candidates.filterValues { stale ->
            stale.baseUrl !in currentUrls &&
                stale.controlPlaneId !in revokedIds &&
                !isGraceExpired(stale.staleSinceEpochMilliseconds, timestamp)
        }
    }

    private fun mergedRoots(
        payload: DirectoryPayload,
        stalePlanes: Map<String, StaleDirectoryPlane>
    ): Map<String, String> = buildMap {
        // Fresh signed entries always come first. Retained stale entries are a
        // fallback only and must not outrank an endpoint from the latest snapshot.
        payload.controlPlanes.forEach { plane -> put(plane.baseUrl, plane.controlPlaneId) }
        stalePlanes.values
            .sortedBy(StaleDirectoryPlane::staleSinceEpochMilliseconds)
            .forEach { stale -> putIfAbsent(stale.baseUrl, stale.controlPlaneId) }
    }

    private fun decodeStalePlanes(encoded: String?): Map<String, StaleDirectoryPlane> {
        if (encoded.isNullOrBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString<List<StaleDirectoryPlane>>(encoded)
                .filter { stale ->
                    stale.baseUrl.startsWith("https://") &&
                        CONTROL_PLANE_ID.matches(stale.controlPlaneId) &&
                        stale.staleSinceEpochMilliseconds >= 0L
                }
                .associateBy(StaleDirectoryPlane::baseUrl)
        }.getOrElse { error ->
            logger.warn { "Ignoring invalid stale Control Plane cache: ${error.message}" }
            emptyMap()
        }
    }

    private suspend fun persistStalePlanes(stalePlanes: Map<String, StaleDirectoryPlane>) {
        dataStore.edit {
            if (stalePlanes.isEmpty()) {
                removeString(KEY_STALE_DISCOVERED_PLANES)
            } else {
                putString(
                    KEY_STALE_DISCOVERED_PLANES,
                    json.encodeToString(stalePlanes.values.toList())
                )
            }
        }
    }

    private fun isGraceExpired(staleSince: Long, currentTime: Long): Boolean =
        currentTime > staleSince && currentTime - staleSince > DISCOVERED_STALE_GRACE_MILLISECONDS

    private fun checkNoRollback(next: DirectoryPayload) {
        val previous = lastVerified ?: return
        require(next.version >= previous.version) { "Control Plane directory version rollback" }
        if (next.version == previous.version) {
            require(
                next.controlPlanes == previous.controlPlanes &&
                    next.revokedControlPlaneIds == previous.revokedControlPlaneIds
            ) { "Directory contents changed without incrementing version" }
        }
        require(next.revokedControlPlaneIds.containsAll(previous.revokedControlPlaneIds)) {
            "A previously revoked Control Plane identity was silently reinstated"
        }
    }

    private companion object {
        const val KEY_SIGNED_ENVELOPE = "transport.control_plane.signed_directory_envelope_v1"
        const val KEY_TRUSTED_DIRECTORY_PUBLIC_KEY = "transport.control_plane.trusted_directory_public_key_v1"
        const val KEY_STALE_DISCOVERED_PLANES = "transport.control_plane.stale_discovered_planes_v1"
        const val DISCOVERED_STALE_GRACE_MILLISECONDS = 24L * 60L * 60L * 1_000L
        val CONTROL_PLANE_ID = Regex("^[0-9a-f]{64}$")
    }
}

@Suppress("UnsafeOptInUsageError")
@Serializable
private data class StaleDirectoryPlane(
    val baseUrl: String,
    val controlPlaneId: String,
    val staleSinceEpochMilliseconds: Long
)
