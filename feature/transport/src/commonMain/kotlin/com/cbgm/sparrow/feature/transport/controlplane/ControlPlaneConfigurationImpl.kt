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
    override val statuses: StateFlow<List<ControlPlaneEndpointStatus>> = _statuses.asStateFlow()

    override suspend fun initialize() = configurationMutex.withLock {
        initializeLocked()
    }

    private suspend fun initializeLocked() {
        if (initialized) return
        _manualBaseUrls.value = dataStore.getString(KEY_MANUAL_CONTROL_PLANES).toUrlSet()
        _directoryBaseUrls.value = dataStore.getString(KEY_DIRECTORY_CONTROL_PLANES).toUrlSet()
        _directoryUrl.value =
            dataStore
                .getString(KEY_DIRECTORY_URL)
                ?.takeIf(String::isNotBlank)
                ?.let(::normalizeHttpUrl)
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

    override suspend fun removeManual(baseUrl: String): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                val normalized = normalizeHttpUrl(baseUrl)
                val updated = _manualBaseUrls.value.filterNot { it == normalized }
                require(updated.isNotEmpty() || _directoryBaseUrls.value.isNotEmpty()) {
                    "At least one control plane must remain"
                }
                persistUrls(KEY_MANUAL_CONTROL_PLANES, updated)
                _manualBaseUrls.value = updated.toSet()
                updateCombinedEndpoints()
                if (normalized !in _manualBaseUrls.value && normalized !in _directoryBaseUrls.value) {
                    forgetPreviousRegistryRoot(normalized)
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

    override suspend fun setDirectoryUrl(url: String?): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                val normalized = url?.takeIf(String::isNotBlank)?.let(::normalizeHttpUrl)
                dataStore.edit {
                    if (normalized == null) {
                        removeString(KEY_DIRECTORY_URL)
                    } else {
                        putString(KEY_DIRECTORY_URL, normalized)
                    }
                }
                _directoryUrl.value = normalized
                if (normalized == null) {
                    persistUrls(KEY_DIRECTORY_CONTROL_PLANES, emptyList())
                    _directoryBaseUrls.value = emptySet()
                    updateCombinedEndpoints()
                }
            }
        }

    override suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit> =
        runCatching {
            configurationMutex.withLock {
                initializeLocked()
                val updated = baseUrls.normalizeUrls()
                persistUrls(KEY_DIRECTORY_CONTROL_PLANES, updated)
                _directoryBaseUrls.value = updated.toSet()
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
        val updated = combineEndpoints(_manualBaseUrls.value, _directoryBaseUrls.value)
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
    }
}

private fun String?.toUrlSet(): Set<String> =
    this
        ?.lineSequence()
        ?.toList()
        ?.normalizeUrls()
        ?.toSet()
        ?: emptySet()

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
    return ControlPlaneEndpoint(normalized).baseUrl
}
