package com.cbgm.sparrow.feature.settings.presentation.network

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.core.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.core.transport.ControlPlaneHealthMonitor
import com.cbgm.sparrow.core.transport.ControlPlaneStatusStore
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.settings.presentation.network.mapper.toUiModels
import com.cbgm.sparrow.feature.settings.presentation.network.mapper.toUiState
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneDirectoryError
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsError
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiState
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ControlPlaneSettingsViewModel(
    private val configuration: ControlPlaneConfiguration,
    private val statusStore: ControlPlaneStatusStore,
    private val healthMonitor: ControlPlaneHealthMonitor,
    private val directorySynchronizer: ControlPlaneDirectorySynchronizer
) : BaseViewModel() {
    private val actionState = MutableStateFlow(ControlPlaneActionState())

    private val configurationSnapshot =
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
        }

    val uiState: StateFlow<ControlPlaneSettingsUiState> =
        combine(
            configurationSnapshot,
            actionState
        ) { configuration, action ->
            configuration.entries.toUiState(
                showAddDialog = action.showAddDialog,
                newUrl = action.newUrl,
                addError = action.addError,
                directoryUrl = configuration.directoryUrl,
                directoryDraft = action.directoryDraft ?: configuration.directoryUrl,
                directoryError = action.directoryError,
                isRefreshing = action.isRefreshing,
                isDirectorySyncing = action.isDirectorySyncing,
                lastDirectoryCount = action.lastDirectoryCount
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ControlPlaneSettingsUiState()
        )

    init {
        startHealthRefresh()
        refreshAll()
    }

    fun onUiEvent(event: ControlPlaneSettingsUiEvent) {
        when (event) {
            ControlPlaneSettingsUiEvent.BackClicked -> navigator.popBackStack()
            ControlPlaneSettingsUiEvent.AddClicked ->
                actionState.update { it.copy(showAddDialog = true, addError = null) }
            ControlPlaneSettingsUiEvent.AddDismissed ->
                actionState.update { it.copy(showAddDialog = false, newUrl = "", addError = null) }
            ControlPlaneSettingsUiEvent.AddConfirmed -> addControlPlane()
            ControlPlaneSettingsUiEvent.DirectoryApply -> saveDirectoryUrl()
            ControlPlaneSettingsUiEvent.Refresh -> refreshAll()
            is ControlPlaneSettingsUiEvent.NewUrlChanged ->
                actionState.update { it.copy(newUrl = event.value, addError = null) }
            is ControlPlaneSettingsUiEvent.DirectoryUrlChanged ->
                actionState.update {
                    it.copy(
                        directoryDraft = event.value,
                        directoryError = null
                    )
                }
            is ControlPlaneSettingsUiEvent.Remove -> removeControlPlane(event.url)
        }
    }

    private fun addControlPlane() {
        val candidate = actionState.value.newUrl.normalizeHttpUrl()
        if (candidate == null) {
            actionState.update { it.copy(addError = ControlPlaneSettingsError.INVALID_URL) }
            return
        }
        if (candidate in configuration.manualBaseUrls.value) {
            actionState.update { it.copy(addError = ControlPlaneSettingsError.DUPLICATE) }
            return
        }

        viewModelScope.launch {
            val directoryResult = directorySynchronizer.synchronizeFrom(candidate)
            if (directoryResult.isSuccess) {
                actionState.update {
                    it.copy(
                        showAddDialog = false,
                        newUrl = "",
                        addError = null,
                        directoryError = null,
                        lastDirectoryCount = directoryResult.getOrThrow()
                    )
                }
                healthMonitor.refresh()
                return@launch
            }

            configuration
                .addManual(candidate)
                .onSuccess {
                    actionState.update {
                        it.copy(showAddDialog = false, newUrl = "", addError = null)
                    }
                    refreshAllNow()
                }.onFailure {
                    actionState.update { it.copy(addError = ControlPlaneSettingsError.SAVE_FAILED) }
                }
        }
    }

    private fun removeControlPlane(url: String) {
        viewModelScope.launch {
            configuration
                .removeManual(url)
                .onSuccess { refreshAllNow() }
                .onFailure {
                    actionState.update { state ->
                        state.copy(addError = ControlPlaneSettingsError.KEEP_ONE)
                    }
                }
        }
    }

    private fun saveDirectoryUrl() {
        val draft = actionState.value.directoryDraft ?: configuration.directoryUrl.value.orEmpty()
        val normalized = draft.takeIf(String::isNotBlank)?.normalizeHttpUrl()
        if (draft.isNotBlank() && normalized == null) {
            actionState.update { it.copy(directoryError = ControlPlaneDirectoryError.INVALID_URL) }
            return
        }

        viewModelScope.launch {
            configuration
                .setDirectoryUrl(normalized)
                .onSuccess {
                    actionState.update { it.copy(directoryDraft = null) }
                    refreshAllNow()
                }.onFailure {
                    actionState.update { state ->
                        state.copy(directoryError = ControlPlaneDirectoryError.SAVE_FAILED)
                    }
                }
        }
    }

    private fun startHealthRefresh() {
        viewModelScope.launch {
            while (isActive) {
                healthMonitor.refresh()
                delay(HEALTH_REFRESH_INTERVAL_MILLISECONDS.milliseconds)
            }
        }
    }

    private fun refreshAll() {
        viewModelScope.launch { refreshAllNow() }
    }

    private suspend fun refreshAllNow() {
        actionState.update {
            it.copy(
                isRefreshing = true,
                directoryError = null
            )
        }

        val directoryResult =
            if (configuration.directoryUrl.value == null) {
                Result.success(0)
            } else {
                actionState.update { it.copy(isDirectorySyncing = true) }
                directorySynchronizer.refresh()
            }

        healthMonitor.refresh()
        actionState.update { current ->
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

    private data class ControlPlaneActionState(
        val showAddDialog: Boolean = false,
        val newUrl: String = "",
        val addError: ControlPlaneSettingsError? = null,
        val directoryDraft: String? = null,
        val directoryError: ControlPlaneDirectoryError? = null,
        val isRefreshing: Boolean = false,
        val isDirectorySyncing: Boolean = false,
        val lastDirectoryCount: Int? = null
    )

    private companion object {
        const val HEALTH_REFRESH_INTERVAL_MILLISECONDS = 1_000L
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
