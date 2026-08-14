package com.cbgm.securechat.feature.transport.controlplane

import android.content.Context
import android.content.SharedPreferences
import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneEndpoint
import com.cbgm.securechat.core.transport.ControlPlaneEndpointStatus
import com.cbgm.securechat.core.transport.ControlPlaneReachability
import com.cbgm.securechat.core.transport.ControlPlaneStatusStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AndroidControlPlaneConfiguration(
    context: Context
) : ControlPlaneConfiguration,
    ControlPlaneStatusStore {
    private val storage = AndroidControlPlaneStorage(context)
    private val _manualBaseUrls = MutableStateFlow(storage.loadManualUrls())
    private val _directoryBaseUrls = MutableStateFlow(storage.loadDirectoryUrls())
    private val _directoryUrl = MutableStateFlow(storage.loadDirectoryUrl())
    private val _endpoints = MutableStateFlow(combineEndpoints(_manualBaseUrls.value, _directoryBaseUrls.value))
    private val _activeEndpoint = MutableStateFlow(_endpoints.value.firstOrNull())
    private val _statuses = MutableStateFlow(createStatuses(_endpoints.value, _activeEndpoint.value))

    override val endpoints: StateFlow<List<ControlPlaneEndpoint>> = _endpoints.asStateFlow()
    override val activeEndpoint: StateFlow<ControlPlaneEndpoint?> = _activeEndpoint.asStateFlow()
    override val manualBaseUrls: StateFlow<Set<String>> = _manualBaseUrls.asStateFlow()
    override val directoryBaseUrls: StateFlow<Set<String>> = _directoryBaseUrls.asStateFlow()
    override val directoryUrl: StateFlow<String?> = _directoryUrl.asStateFlow()
    override val statuses: StateFlow<List<ControlPlaneEndpointStatus>> = _statuses.asStateFlow()

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
            val updated = baseUrls.normalizeUrls()
            require(updated.isNotEmpty()) { "At least one control-plane URL is required" }
            storage.persistManualUrls(updated)
            _manualBaseUrls.value = updated.toSet()
            updateCombinedEndpoints()
        }

    override suspend fun addManual(baseUrl: String): Result<Unit> =
        runCatching {
            val normalized = normalizeHttpUrl(baseUrl)
            val updated = (_manualBaseUrls.value + normalized).toList()
            storage.persistManualUrls(updated)
            _manualBaseUrls.value = updated.toSet()
            updateCombinedEndpoints()
        }

    override suspend fun removeManual(baseUrl: String): Result<Unit> =
        runCatching {
            val updated = _manualBaseUrls.value.filterNot { it == baseUrl }
            require(updated.isNotEmpty() || _directoryBaseUrls.value.isNotEmpty()) {
                "At least one control plane must remain"
            }
            storage.persistManualUrls(updated)
            _manualBaseUrls.value = updated.toSet()
            updateCombinedEndpoints()
        }

    override suspend fun setDirectoryUrl(url: String?): Result<Unit> =
        runCatching {
            val normalized = url?.takeIf(String::isNotBlank)?.let(::normalizeHttpUrl)
            storage.persistDirectoryUrl(normalized)
            _directoryUrl.value = normalized
            if (normalized == null) {
                storage.persistDirectoryUrls(emptyList())
                _directoryBaseUrls.value = emptySet()
                updateCombinedEndpoints()
            }
        }

    override suspend fun replaceDirectory(baseUrls: List<String>): Result<Unit> =
        runCatching {
            val updated = baseUrls.normalizeUrls()
            storage.persistDirectoryUrls(updated)
            _directoryBaseUrls.value = updated.toSet()
            updateCombinedEndpoints()
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
}

private class AndroidControlPlaneStorage(
    context: Context
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadManualUrls(): Set<String> =
        preferences
            .getString(KEY_MANUAL_CONTROL_PLANES, null)
            ?.lineSequence()
            ?.toList()
            ?.normalizeUrls()
            ?.toSet()
            ?: emptySet()

    fun loadDirectoryUrls(): Set<String> =
        preferences
            .getString(KEY_DIRECTORY_CONTROL_PLANES, null)
            ?.lineSequence()
            ?.toList()
            ?.normalizeUrls()
            ?.toSet()
            ?: emptySet()

    fun loadDirectoryUrl(): String? =
        preferences
            .getString(KEY_DIRECTORY_URL, null)
            ?.takeIf(String::isNotBlank)
            ?.let(::normalizeHttpUrl)

    fun persistManualUrls(urls: List<String>) {
        persistUrls(KEY_MANUAL_CONTROL_PLANES, urls)
    }

    fun persistDirectoryUrls(urls: List<String>) {
        persistUrls(KEY_DIRECTORY_CONTROL_PLANES, urls)
    }

    fun persistDirectoryUrl(url: String?) {
        val persisted = preferences.edit().putString(KEY_DIRECTORY_URL, url).commit()
        check(persisted) { "Control-plane directory URL could not be persisted" }
    }

    private fun persistUrls(
        key: String,
        urls: List<String>
    ) {
        val persisted = preferences.edit().putString(key, urls.joinToString(separator = "\n")).commit()
        check(persisted) { "Control-plane configuration could not be persisted" }
    }

    private companion object {
        const val PREFERENCES_NAME = "securechat_control_planes"
        const val KEY_MANUAL_CONTROL_PLANES = "manual_base_urls"
        const val KEY_DIRECTORY_CONTROL_PLANES = "directory_base_urls"
        const val KEY_DIRECTORY_URL = "directory_url"
    }
}

private fun createStatuses(
    endpoints: List<ControlPlaneEndpoint>,
    activeEndpoint: ControlPlaneEndpoint?
): List<ControlPlaneEndpointStatus> =
    endpoints.map { endpoint ->
        ControlPlaneEndpointStatus(
            endpoint = endpoint,
            isActive = endpoint == activeEndpoint
        )
    }

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
