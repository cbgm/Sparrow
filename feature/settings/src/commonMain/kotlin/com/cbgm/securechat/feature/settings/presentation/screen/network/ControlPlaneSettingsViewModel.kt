package com.cbgm.securechat.feature.settings.presentation.screen

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.transport.ControlPlaneConfiguration
import com.cbgm.securechat.core.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.securechat.core.transport.ControlPlaneEndpoint
import com.cbgm.securechat.core.transport.ControlPlaneEndpointStatus
import com.cbgm.securechat.core.transport.ControlPlaneHealthMonitor
import com.cbgm.securechat.core.transport.ControlPlaneReachability
import com.cbgm.securechat.core.transport.ControlPlaneStatusStore
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneDirectoryError
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneSettingsError
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneSettingsUiEvent
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneSettingsUiState
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneUiModel
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneUiSource
import com.cbgm.securechat.feature.settings.presentation.model.ControlPlaneUiStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ControlPlaneSettingsViewModel(
    private val configuration: ControlPlaneConfiguration,
    private val statusStore: ControlPlaneStatusStore,
    private val healthMonitor: ControlPlaneHealthMonitor,
    private val directorySynchronizer: ControlPlaneDirectorySynchronizer
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(ControlPlaneSettingsUiState())
    val uiState: StateFlow<ControlPlaneSettingsUiState> = _uiState.asStateFlow()

    init {
        observeConfiguration()
        startHealthRefresh()
        refreshAll()
    }

    fun onUiEvent(event: ControlPlaneSettingsUiEvent) {
        when (event) {
            ControlPlaneSettingsUiEvent.BackClicked -> navigator.popBackStack()
            ControlPlaneSettingsUiEvent.AddClicked ->
                _uiState.update { it.copy(showAddDialog = true, addError = null) }
            ControlPlaneSettingsUiEvent.AddDismissed ->
                _uiState.update { it.copy(showAddDialog = false, newUrl = "", addError = null) }
            ControlPlaneSettingsUiEvent.AddConfirmed -> addControlPlane()
            ControlPlaneSettingsUiEvent.DirectoryApply -> saveDirectoryUrl()
            ControlPlaneSettingsUiEvent.Refresh -> refreshAll()
            is ControlPlaneSettingsUiEvent.NewUrlChanged ->
                _uiState.update { it.copy(newUrl = event.value, addError = null) }
            is ControlPlaneSettingsUiEvent.DirectoryUrlChanged ->
                _uiState.update { it.copy(directoryDraft = event.value, directoryError = null) }
            is ControlPlaneSettingsUiEvent.Remove -> removeControlPlane(event.url)
        }
    }

    private fun addControlPlane() {
        val candidate = _uiState.value.newUrl.normalizeHttpUrl()
        if (candidate == null) {
            _uiState.update { it.copy(addError = ControlPlaneSettingsError.INVALID_URL) }
            return
        }
        if (candidate in configuration.manualBaseUrls.value) {
            _uiState.update { it.copy(addError = ControlPlaneSettingsError.DUPLICATE) }
            return
        }
        viewModelScope.launch {
            configuration
                .addManual(candidate)
                .onSuccess {
                    _uiState.update {
                        it.copy(showAddDialog = false, newUrl = "", addError = null)
                    }
                    refreshAllNow()
                }.onFailure {
                    _uiState.update { it.copy(addError = ControlPlaneSettingsError.SAVE_FAILED) }
                }
        }
    }

    private fun removeControlPlane(url: String) {
        viewModelScope.launch {
            configuration
                .removeManual(url)
                .onSuccess { refreshAllNow() }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(addError = ControlPlaneSettingsError.KEEP_ONE)
                    }
                }
        }
    }

    private fun saveDirectoryUrl() {
        val draft = _uiState.value.directoryDraft
        val normalized = draft.takeIf(String::isNotBlank)?.normalizeHttpUrl()
        if (draft.isNotBlank() && normalized == null) {
            _uiState.update { it.copy(directoryError = ControlPlaneDirectoryError.INVALID_URL) }
            return
        }
        viewModelScope.launch {
            configuration
                .setDirectoryUrl(normalized)
                .onSuccess { refreshAllNow() }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(directoryError = ControlPlaneDirectoryError.SAVE_FAILED)
                    }
                }
        }
    }

    private fun observeConfiguration() {
        viewModelScope.launch {
            combine(
                statusStore.statuses,
                configuration.manualBaseUrls,
                configuration.directoryBaseUrls,
                configuration.directoryUrl
            ) { statuses, manual, directory, directoryUrl ->
                ConfigurationSnapshot(
                    entries = statuses.toUiModels(manual, directory),
                    directoryUrl = directoryUrl.orEmpty()
                )
            }.collect { snapshot ->
                _uiState.update { current ->
                    current.copy(
                        entries = snapshot.entries,
                        directoryUrl = snapshot.directoryUrl,
                        directoryDraft =
                            if (current.directoryDraft == current.directoryUrl) {
                                snapshot.directoryUrl
                            } else {
                                current.directoryDraft
                            }
                    )
                }
            }
        }
    }

    private fun startHealthRefresh() {
        viewModelScope.launch {
            while (isActive) {
                healthMonitor.refresh()
                delay(HEALTH_REFRESH_INTERVAL_MILLISECONDS)
            }
        }
    }

    private fun refreshAll() {
        viewModelScope.launch { refreshAllNow() }
    }

    private suspend fun refreshAllNow() {
        _uiState.update { it.copy(isRefreshing = true, directoryError = null) }
        val directoryResult =
            if (configuration.directoryUrl.value == null) {
                Result.success(0)
            } else {
                _uiState.update { it.copy(isDirectorySyncing = true) }
                directorySynchronizer.refresh()
            }
        healthMonitor.refresh()
        _uiState.update { current ->
            current.copy(
                isRefreshing = false,
                isDirectorySyncing = false,
                lastDirectoryCount = directoryResult.getOrNull() ?: current.lastDirectoryCount,
                directoryError =
                    if (directoryResult.isFailure) {
                        ControlPlaneDirectoryError.SYNC_FAILED
                    } else {
                        null
                    }
            )
        }
    }

    private companion object {
        const val HEALTH_REFRESH_INTERVAL_MILLISECONDS = 30_000L
    }
}

private data class ConfigurationSnapshot(
    val entries: List<ControlPlaneUiModel>,
    val directoryUrl: String
)

private fun String.normalizeHttpUrl(): String? {
    val trimmed = trim().trimEnd('/')
    if (trimmed.isBlank() || trimmed.any(Char::isWhitespace)) return null
    val normalized =
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    return runCatching { ControlPlaneEndpoint(normalized).baseUrl }.getOrNull()
}

private fun List<ControlPlaneEndpointStatus>.toUiModels(
    manual: Set<String>,
    directory: Set<String>
): List<ControlPlaneUiModel> =
    map { status ->
        ControlPlaneUiModel(
            url = status.endpoint.baseUrl,
            status = status.toUiStatus(),
            source = sourceFor(status.endpoint.baseUrl, manual, directory),
            canRemove = canRemove(status.endpoint.baseUrl, manual, directory)
        )
    }.sortedWith(
        compareBy<ControlPlaneUiModel> { it.status.sortOrder() }
            .thenBy(ControlPlaneUiModel::url)
    )

private fun ControlPlaneEndpointStatus.toUiStatus(): ControlPlaneUiStatus =
    when {
        reachability == ControlPlaneReachability.UNREACHABLE -> ControlPlaneUiStatus.UNREACHABLE
        isActive && reachability == ControlPlaneReachability.AVAILABLE -> ControlPlaneUiStatus.ACTIVE
        reachability == ControlPlaneReachability.AVAILABLE -> ControlPlaneUiStatus.AVAILABLE
        else -> ControlPlaneUiStatus.CHECKING
    }

private fun sourceFor(
    url: String,
    manual: Set<String>,
    directory: Set<String>
): ControlPlaneUiSource =
    when {
        url in manual && url in directory -> ControlPlaneUiSource.MANUAL_AND_DIRECTORY
        url in directory -> ControlPlaneUiSource.DIRECTORY
        else -> ControlPlaneUiSource.MANUAL
    }

private fun canRemove(
    url: String,
    manual: Set<String>,
    directory: Set<String>
): Boolean {
    if (url !in manual) return false
    return ((manual - url) + directory).isNotEmpty()
}

private fun ControlPlaneUiStatus.sortOrder(): Int =
    when (this) {
        ControlPlaneUiStatus.ACTIVE -> 0
        ControlPlaneUiStatus.AVAILABLE -> 1
        ControlPlaneUiStatus.CHECKING -> 2
        ControlPlaneUiStatus.UNREACHABLE -> 3
    }
