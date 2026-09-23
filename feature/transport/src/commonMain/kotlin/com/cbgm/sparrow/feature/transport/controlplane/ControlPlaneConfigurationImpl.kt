package com.cbgm.sparrow.feature.transport.controlplane

import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.core.transport.ControlPlaneEndpointStatus
import com.cbgm.sparrow.core.transport.ControlPlaneReachability
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.data.datastore.SparrowDataStore
import com.cbgm.sparrow.feature.transport.discovery.NodeDirectoryCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ControlPlaneConfigurationImpl(
    private val dataStore: SparrowDataStore,
    private val nodeDirectoryCache: NodeDirectoryCache
) : ControlPlaneConfiguration,
    ControlPlaneStatusStore {
    private val _manualBaseUrls = MutableStateFlow(emptySet<String>())
    private val _directoryBaseUrls = MutableStateFlow(emptySet<String>())
    private val _directoryUrl = MutableStateFlow<String?>(null)
    private val _jsonDirectoryUrl = MutableStateFlow<String?>(null)
    private val _jsonDirectoryBaseUrls = MutableStateFlow(emptySet<String>())

    // Node-advertised URL hints are NOT equivalent to signed-directory identities.
    private val nodeHintBaseUrls = MutableStateFlow(emptySet<String>())
    private val verifiedDirectoryRoots = MutableStateFlow(emptyMap<String, String>())
    private val _endpoints = MutableStateFlow(emptyList<ControlPlaneEndpoint>())
    private val _activeEndpoint = MutableStateFlow<ControlPlaneEndpoint?>(null)
    private val _statuses = MutableStateFlow(emptyList<ControlPlaneEndpointStatus>())
    private var initialized = false

    // Initialize and edit the persisted endpoint set in the same critical section.
    // Otherwise startup and consecutive remove/add requests can overwrite each other.
    private val configurationMutex = Mutex()

    override val endpoints: StateFlow<List<ControlPlaneEndpoint>> = _endpoints.asStateFlow()
    override val activeEndpoint: StateFlow<ControlPlaneEndpoint?> = _activeEndpoint.asStateFlow()
    override val manualBaseUrls: StateFlow<Set<String>> = _manualBaseUrls.asStateFlow()
    override val directoryBaseUrls: StateFlow<Set<String>> = _directoryBaseUrls.asStateFlow()
    override val directoryUrl: StateFlow<String?> = _directoryUrl.asStateFlow()
    override val jsonDirectoryUrl: StateFlow<String?> = _jsonDirectoryUrl.asStateFlow()
    override val jsonDirectoryBaseUrls: StateFlow<Set<String>> = _jsonDirectoryBaseUrls.asStateFlow()
    override val statuses: StateFlow<List<ControlPlaneEndpointStatus>> = _statuses.asStateFlow()

    override suspend fun initialize() = configurationMutex.withLock {
        initializeLocked()
    }

    private suspend fun initializeLocked() {
        if (initialized) return
        _manualBaseUrls.value = dataStore.getString(KEY_MANUAL_CONTROL_PLANES).toUrlSet()
        // Legacy URL-only signed-directory entries have no authenticated source.
        _directoryBaseUrls.value = emptySet()
        // Old builds stored imported JSON addresses as a live directory feed.
        // Import them once into the manual set; never refresh that feed again,
        // otherwise explicitly removed addresses would be silently re-added.
        val legacyJsonUrl = dataStore.getString(KEY_JSON_DIRECTORY_URL)
        val legacyJsonEntries = dataStore.getString(KEY_JSON_DIRECTORY_CONTROL_PLANES).toUrlSet()
        // Previous builds could persist a malformed directory URL (or a JSON
        // document URL) in this field. Do not let that obsolete setting abort
        // transport initialization, push-token registration or app startup.
        // Restore the signed offline cache independently after initialization.
        val savedDirectory = dataStore.getString(KEY_DIRECTORY_URL)
            ?.takeIf(String::isNotBlank)
        _directoryUrl.value = savedDirectory?.let { saved ->
            runCatching { normalizeHttpUrl(saved) }
                .getOrNull()
                ?.takeIf { it.startsWith("https://") }
        }
        if (savedDirectory != null && _directoryUrl.value == null) {
            dataStore.edit { removeString(KEY_DIRECTORY_URL) }
        }
        if (legacyJsonUrl != null || legacyJsonEntries.isNotEmpty()) {
            val imported = legacyJsonEntries - setOfNotNull(_directoryUrl.value)
            val merged = _manualBaseUrls.value + imported
            dataStore.edit {
                putString(KEY_MANUAL_CONTROL_PLANES, merged.joinToString("\n"))
                removeString(KEY_JSON_DIRECTORY_URL)
                removeString(KEY_JSON_DIRECTORY_CONTROL_PLANES)
            }
            _manualBaseUrls.value = merged
        }
        _jsonDirectoryUrl.value = null
        _jsonDirectoryBaseUrls.value = emptySet()
        // A previous build may have saved the signed directory host as a manual
        // Control Plane, causing erroneous GET /v1/nodes against the directory.
        // Do not keep that accidental duplicate in the active endpoint list.
        _directoryUrl.value?.let { directory ->
            if (directory in _manualBaseUrls.value) {
                val cleaned = _manualBaseUrls.value - directory
                persistUrls(KEY_MANUAL_CONTROL_PLANES, cleaned.toList())
                _manualBaseUrls.value = cleaned
            }
        }
        updateCombinedEndpoints()
        initialized = true
    }

    override fun orderedEndpoints(): List<ControlPlaneEndpoint> {
        val configured = _endpoints.value
        val statusByEndpoint = _statuses.value.associateBy(ControlPlaneEndpointStatus::endpoint)
        val reachable =
            configured.filter { endpoint ->
                statusByEndpoint[endpoint]?.reachability != ControlPlaneReachability.UNREACHABLE
            }
        val unreachable = configured.filterNot(reachable::contains)
        val active = _activeEndpoint.value?.takeIf(reachable::contains)
        return listOfNotNull(active) + reachable.filterNot { it == active } + unreachable
    }

    override fun markActive(endpoint: ControlPlaneEndpoint) {
        if (endpoint !in _endpoints.value) return
        _activeEndpoint.value = endpoint
        markAvailable(endpoint)
        _statuses.update { current ->
            current.map { status -> status.copy(isActive = status.endpoint == endpoint) }
        }
    }

    override fun markAvailable(endpoint: ControlPlaneEndpoint) {
        updateReachability(endpoint, ControlPlaneReachability.AVAILABLE)
    }

    override fun markUnreachable(endpoint: ControlPlaneEndpoint) {
        updateReachability(endpoint, ControlPlaneReachability.UNREACHABLE)
    }

    override suspend fun replace(baseUrls: List<String>): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                val updated = baseUrls.normalizeUrls()
                require(updated.isNotEmpty()) { "At least one control-plane URL is required" }
                persistUrls(KEY_MANUAL_CONTROL_PLANES, updated)
                _manualBaseUrls.value = updated.toSet()
                updateCombinedEndpoints()
            }
        }

    override suspend fun addManual(baseUrl: String): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                val normalized = normalizeHttpUrl(baseUrl)
                val updated = (_manualBaseUrls.value + normalized).toList()
                persistUrls(KEY_MANUAL_CONTROL_PLANES, updated)
                _manualBaseUrls.value = updated.toSet()
                updateCombinedEndpoints()
            }
        }

    override suspend fun importManual(baseUrls: List<String>): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                require(baseUrls.isNotEmpty() && baseUrls.size <= 64) {
                    "JSON list is empty or too large"
                }
                val imported = baseUrls.normalizeUrls().toSet()
                _directoryUrl.value?.let { directoryUrl ->
                    require(directoryUrl !in imported) {
                        "JSON list contains the Directory Server URL rather than a Control Plane"
                    }
                }
                val updated = _manualBaseUrls.value + imported
                // One durable write for the entire import. Do not touch the
                // signed directory URL, its verified members, or its trust pins.
                persistUrls(KEY_MANUAL_CONTROL_PLANES, updated.toList())
                _manualBaseUrls.value = updated
                updateCombinedEndpoints()
            }
        }

    override suspend fun removeManual(baseUrl: String): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                val normalized = normalizeHttpUrl(baseUrl)
                val updated = _manualBaseUrls.value.filterNot { it == normalized }
                // Removing the final manual entry is a valid explicit user action.
                // A configured directory may be offline or have no approved planes yet.
                // Never trap the user in an invalid/obsolete manual configuration.
                persistUrls(KEY_MANUAL_CONTROL_PLANES, updated)
                _manualBaseUrls.value = updated.toSet()
                updateCombinedEndpoints()
                if (normalized !in _manualBaseUrls.value && normalized !in _directoryBaseUrls.value &&
                    normalized !in _jsonDirectoryBaseUrls.value
                ) {
                    try {
                        forgetPreviousRegistryRoot(normalized)
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Manual URL is already removed and persisted. Keep the
                        // existing pin if the old cache cannot be updated.
                    }
                }
            }
        }

    private suspend fun forgetPreviousRegistryRoot(baseUrl: String) {
        val cached = nodeDirectoryCache.read() ?: return
        // Keep other servers' pinned roots but invalidate the removed server's
        // old signed directory as a fallback. Never run this automatically on
        // signature mismatch; only explicit removal can reset trust.
        nodeDirectoryCache.write(
            cached.copy(
                sourceControlPlaneBaseUrl =
                    if (cached.sourceControlPlaneBaseUrl == baseUrl ||
                        cached.sourceControlPlaneBaseUrl == null
                    ) {
                        "removed:$baseUrl"
                    } else {
                        cached.sourceControlPlaneBaseUrl
                    },
                trustedRootsByControlPlane = cached.trustedRootsByControlPlane - baseUrl
            )
        )
    }

    override suspend fun useDefaultDirectoryUrlIfUnconfigured(url: String): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                // A build-time URL is a one-time bootstrap, never a recurring
                // fallback. Once a user edits/removes the directory, changing
                // local.properties or installing a new APK must not restore it.
                if (dataStore.getString(KEY_DIRECTORY_BOOTSTRAP_DONE) == "1") return@withLock
                // Respect directories configured by previous app versions, as
                // well as previous removals and an already-consumed build default.
                val existingChoice =
                    dataStore.getString(KEY_USER_DIRECTORY_CHOICE) != null ||
                        dataStore.getString(KEY_LAST_BUILD_DIRECTORY_URL) != null ||
                        _directoryUrl.value != null || _jsonDirectoryUrl.value != null
                val initialUrl = if (existingChoice) {
                    null
                } else {
                    url.trim().takeIf(String::isNotBlank)?.let(::normalizeHttpUrl)
                }
                require(initialUrl == null || initialUrl.startsWith("https://")) {
                    "Signed directory requires HTTPS"
                }
                dataStore.edit {
                    putString(KEY_DIRECTORY_BOOTSTRAP_DONE, "1")
                    if (!existingChoice && initialUrl != null) {
                        putString(KEY_DIRECTORY_URL, initialUrl)
                    }
                }
                if (!existingChoice && initialUrl != null) _directoryUrl.value = initialUrl
            }
        }

    override suspend fun setDirectoryUrl(url: String?): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                val normalized = url?.takeIf(String::isNotBlank)?.let(::normalizeHttpUrl)
                require(normalized == null || normalized.startsWith("https://")) {
                    "Signed directory requires HTTPS"
                }
                val lastBuildDefault = dataStore.getString(KEY_LAST_BUILD_DIRECTORY_URL)
                dataStore.edit {
                    // An explicit Remove must survive app restart even with a build-time default URL.
                    val explicitlyRemoved = normalized == null && _jsonDirectoryUrl.value == null
                    putString(KEY_USER_DIRECTORY_CHOICE, if (explicitlyRemoved) "removed" else "1")
                    putString(KEY_DIRECTORY_BOOTSTRAP_DONE, "1")
                    if (explicitlyRemoved) {
                        (_directoryUrl.value ?: lastBuildDefault)?.let {
                            putString(KEY_REMOVED_DIRECTORY_DEFAULT_URL, it)
                        }
                    } else {
                        removeString(KEY_REMOVED_DIRECTORY_DEFAULT_URL)
                    }
                    if (normalized == null) {
                        removeString(KEY_DIRECTORY_URL)
                    } else {
                        putString(KEY_DIRECTORY_URL, normalized)
                    }
                }
                _directoryUrl.value = normalized
                if (normalized != null) {
                    // Before the first signed snapshot, node-discovered URL hints
                    // may exist without any signed identity. They must not be
                    // misclassified as authenticated entries in the central list.
                    nodeHintBaseUrls.value = emptySet()
                    _directoryBaseUrls.value = _directoryBaseUrls.value
                        .filterTo(mutableSetOf()) { it in verifiedDirectoryRoots.value }
                    updateCombinedEndpoints()
                }
                if (normalized != null && normalized in _manualBaseUrls.value) {
                    val remaining = _manualBaseUrls.value - normalized
                    persistUrls(KEY_MANUAL_CONTROL_PLANES, remaining.toList())
                    _manualBaseUrls.value = remaining
                    updateCombinedEndpoints()
                }
                // Clearing/changing the active directory URL is not a trust
                // reset. Preserve signed cache and identity bindings during
                // source edits and outages. Explicit Remove Directory handles
                // wiping the signing trust in the synchronizer.
            }
        }

    // Compatibility for old callers: importing a JSON list always adds removable
    // manual entries, it must never replace the signed directory configuration.
    override suspend fun replaceJsonDirectory(url: String, baseUrls: List<String>): Result<Unit> =
        importManual(baseUrls)

    override suspend fun clearJsonDirectory(): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                dataStore.edit {
                    removeString(KEY_JSON_DIRECTORY_URL)
                }
                _jsonDirectoryUrl.value = null
                // An edited source must not make already discovered entries
                // vanish before the replacement source has been verified.
                updateCombinedEndpoints()
            }
        }

    override suspend fun clearJsonDirectoryCache(): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                dataStore.edit { removeString(KEY_JSON_DIRECTORY_CONTROL_PLANES) }
                _jsonDirectoryBaseUrls.value = emptySet()
                updateCombinedEndpoints()
            }
        }

    override fun verifiedDirectoryRootId(baseUrl: String): String? =
        verifiedDirectoryRoots.value[baseUrl]

    override suspend fun replaceVerifiedDirectory(rootsByBaseUrl: Map<String, String>): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                val normalized = rootsByBaseUrl.keys.toList().normalizeUrls()
                require(normalized.size == rootsByBaseUrl.size) { "Duplicate verified directory URL" }
                require(normalized.all { it.startsWith("https://") }) { "Signed directory requires HTTPS" }
                persistUrls(KEY_DIRECTORY_CONTROL_PLANES, normalized)
                verifiedDirectoryRoots.value = rootsByBaseUrl
                nodeHintBaseUrls.value = emptySet()
                _directoryBaseUrls.value = normalized.toSet()
                updateCombinedEndpoints()
            }
        }

    override suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                val updated = baseUrls.normalizeUrls()
                // Node-advertised URL hints must not inject a signing root or
                // resurrect a plane revoked by the signed central directory.
                require(_directoryUrl.value == null) {
                    "Unsigned directory hints are disabled when signed discovery is configured"
                }
                // These are node-advertised *hints* only; no signed identity exists.
                // Keep them separate from the authenticated central directory.
                nodeHintBaseUrls.value = updated.toSet()
                updateCombinedEndpoints()
            }
        }

    private suspend fun persistUrls(
        key: String,
        urls: List<String>
    ) {
        dataStore.edit { putString(key, urls.joinToString(separator = "\n")) }
    }

    private fun updateCombinedEndpoints() {
        val previous = _statuses.value.associateBy { status -> status.endpoint.baseUrl }
        val updated = combineEndpoints(
            _manualBaseUrls.value,
            _directoryBaseUrls.value + _jsonDirectoryBaseUrls.value + nodeHintBaseUrls.value
        )
        val active = _activeEndpoint.value?.takeIf(updated::contains) ?: updated.firstOrNull()
        _endpoints.value = updated
        _activeEndpoint.value = active
        _statuses.value =
            updated.map { endpoint ->
                ControlPlaneEndpointStatus(
                    endpoint = endpoint,
                    reachability = previous[endpoint.baseUrl]?.reachability ?: ControlPlaneReachability.UNKNOWN,
                    isActive = endpoint == active
                )
            }
    }

    private fun updateReachability(
        endpoint: ControlPlaneEndpoint,
        reachability: ControlPlaneReachability
    ) {
        _statuses.update { current ->
            current.map { status ->
                if (status.endpoint == endpoint) status.copy(reachability = reachability) else status
            }
        }
    }

    private companion object {
        const val PREFIX = "transport.control_plane."
        const val KEY_MANUAL_CONTROL_PLANES = "${PREFIX}manual_base_urls"
        const val KEY_DIRECTORY_CONTROL_PLANES = "${PREFIX}directory_base_urls"
        const val KEY_DIRECTORY_URL = "${PREFIX}directory_url"
        const val KEY_JSON_DIRECTORY_URL = "${PREFIX}json_directory_url"
        const val KEY_JSON_DIRECTORY_CONTROL_PLANES = "${PREFIX}json_directory_base_urls"
        const val KEY_DIRECTORY_BOOTSTRAP_DONE = "${PREFIX}directory_bootstrap_done_v1"
        const val KEY_USER_DIRECTORY_CHOICE = "${PREFIX}user_directory_choice_v1"
        const val KEY_REMOVED_DIRECTORY_DEFAULT_URL = "${PREFIX}removed_directory_default_url_v1"
        const val KEY_LAST_BUILD_DIRECTORY_URL = "${PREFIX}last_build_directory_url_v1"
    }
}

private fun String?.toUrlSet(): Set<String> =
    this?.lineSequence()?.mapNotNull { value ->
        runCatching { normalizeHttpUrl(value) }.getOrNull()?.takeIf { url ->
            CONTROL_PLANE_ORIGIN.matches(url)
        }
    }?.toSet() ?: emptySet()

private fun combineEndpoints(
    manual: Set<String>,
    directory: Set<String>
): List<ControlPlaneEndpoint> =
    (manual.toList() + directory)
        .distinct()
        .map(::ControlPlaneEndpoint)

private fun List<String>.normalizeUrls(): List<String> =
    map(String::trim)
        .filter(String::isNotBlank)
        .map(::normalizeHttpUrl)
        .distinct()

private val CONTROL_PLANE_ORIGIN = Regex("^https?://[A-Za-z0-9.-]+(?::[0-9]{1,5})?$")

private fun normalizeHttpUrl(value: String): String {
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
