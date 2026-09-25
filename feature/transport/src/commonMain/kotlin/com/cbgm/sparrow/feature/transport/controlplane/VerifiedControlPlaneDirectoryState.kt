package com.cbgm.sparrow.feature.transport.controlplane

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Owns the pinned directory identity, anti-rollback state and authenticated offline snapshot. */
internal class VerifiedControlPlaneDirectoryState(
    private val configuration: ControlPlaneConfiguration,
    private val verifier: SignedControlPlaneDirectoryVerifier,
    private val dataStore: SparrowDataStore,
    private val json: Json
) {
    private val logger = SparrowLog.withTag("ControlPlaneDirectory")
    private var loaded = false
    private var lastVerified: DirectoryPayload? = null
    private var trustedPublicKey: String? = null

    val hasCachedSnapshot: Boolean get() = lastVerified != null

    suspend fun restore() {
        if (loaded) return
        val storedKey = dataStore.getString(KEY_TRUSTED_DIRECTORY_PUBLIC_KEY)
        val storedEnvelope = dataStore.getString(KEY_SIGNED_ENVELOPE)
        loaded = true
        // Never replace an existing pin merely because its cached envelope is corrupt.
        trustedPublicKey = storedKey?.takeIf(String::isNotBlank)
        if (trustedPublicKey == null || storedEnvelope == null) return
        try {
            val payload = verifier.verify(storedEnvelope, trustedPublicKey!!, allowExpired = true)
            apply(payload)
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

        // Save the trusted key only after validating the complete signed snapshot.
        dataStore.edit {
            putString(KEY_TRUSTED_DIRECTORY_PUBLIC_KEY, download.publicKey)
            putString(KEY_SIGNED_ENVELOPE, download.envelope)
        }
        trustedPublicKey = download.publicKey
        apply(payload)
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
        }
        // Called under the synchronizer mutex: a pending refresh cannot restore removed entries.
        configuration.replaceVerifiedDirectory(emptyMap()).getOrThrow()
        lastVerified = null
        trustedPublicKey = null
        loaded = true
    }

    private suspend fun apply(payload: DirectoryPayload) {
        configuration.replaceVerifiedDirectory(
            payload.controlPlanes.associate { it.baseUrl to it.controlPlaneId }
        ).getOrThrow()
        lastVerified = payload
    }

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
    }
}
