package com.cbgm.sparrow.feature.settings.presentation.network

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.core.transport.ControlPlaneDirectorySynchronizer
import com.cbgm.sparrow.core.transport.ControlPlaneEndpoint
import com.cbgm.sparrow.core.transport.ControlPlaneHealthMonitor
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.settings.domain.usecase.ObserveControlPlaneSettingsContextUseCase
import com.cbgm.sparrow.feature.settings.presentation.network.mapper.toControlPlaneSettingsUiState
import com.cbgm.sparrow.feature.settings.presentation.network.mapper.toControlPlaneUiModels
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneDirectoryError
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsError
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiEvent
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneSettingsUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

class ControlPlaneSettingsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val configuration: ControlPlaneConfiguration,
    observeControlPlaneSettingsContext: ObserveControlPlaneSettingsContextUseCase,
    private val healthMonitor: ControlPlaneHealthMonitor,
    private val directorySynchronizer: ControlPlaneDirectorySynchronizer
) : BaseViewModel() {
    private val showAddDialog =
        savedStateHandle.getMutableStateFlow(SHOW_ADD_DIALOG_KEY, false)
    private val newUrl =
        savedStateHandle.getMutableStateFlow(NEW_URL_KEY, "")
    private val directoryDraft =
        MutableStateFlow(savedStateHandle.get<String>(DIRECTORY_DRAFT_KEY))
    private val actionState = MutableStateFlow(ControlPlaneActionState())

    // Serialize remove, add and directory refresh. A user may tap Add immediately
    // after Remove; those operations must not race the automatic refresh.
    private val configurationActionMutex = Mutex()
    private val formState =
        combine(showAddDialog, newUrl, directoryDraft) { showAddDialog, newUrl, directoryDraft ->
            ControlPlaneFormState(
                showAddDialog = showAddDialog,
                newUrl = newUrl,
                directoryDraft = directoryDraft
            )
        }

    private val configurationContext = observeControlPlaneSettingsContext()

    val uiState: StateFlow<ControlPlaneSettingsUiState> =
        combine(
            configurationContext,
            formState,
            actionState
        ) { configuration, form, action ->
            configuration.statuses
                .toControlPlaneUiModels(configuration.manualBaseUrls, configuration.directoryBaseUrls)
                .toControlPlaneSettingsUiState(
                    showAddDialog = form.showAddDialog,
                    newUrl = form.newUrl,
                    addError = action.addError,
                    directoryUrl = configuration.directoryUrl,
                    directoryDraft = form.directoryDraft ?: configuration.directoryUrl,
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
            ControlPlaneSettingsUiEvent.AddClicked -> openAddDialog()
            ControlPlaneSettingsUiEvent.AddDismissed -> closeAddDialog()
            ControlPlaneSettingsUiEvent.AddConfirmed -> addControlPlane()
            ControlPlaneSettingsUiEvent.DirectoryApply -> saveDirectoryUrl()
            ControlPlaneSettingsUiEvent.Refresh -> refreshAll()
            is ControlPlaneSettingsUiEvent.NewUrlChanged -> updateNewUrl(event.value)
            is ControlPlaneSettingsUiEvent.Remove -> removeControlPlane(event.url)
        }
    }

    private fun openAddDialog() {
        showAddDialog.value = true
        actionState.update { state -> state.copy(addError = null) }
    }

    private fun closeAddDialog() {
        clearAddDialogForm()
        actionState.update { state -> state.copy(addError = null) }
    }

    private fun updateNewUrl(value: String) {
        newUrl.value = value
        actionState.update { state -> state.copy(addError = null) }
    }

    private fun addControlPlane() {
        viewModelScope.launch {
            configurationActionMutex.withLock {
                // Read the value after any pending removal has completed.
                val candidate = newUrl.value.normalizeHttpUrl()
                if (candidate == null) {
                    actionState.update { it.copy(addError = ControlPlaneSettingsError.INVALID_URL) }
                    return@withLock
                }
                if (candidate in configuration.manualBaseUrls.value) {
                    actionState.update { it.copy(addError = ControlPlaneSettingsError.DUPLICATE) }
                    return@withLock
                }

                // A JSON directory URL remains supported by the Add field. A normal
                // Control Plane base URL is added manually if it is not a directory.
                val directoryResult = directorySynchronizer.synchronizeFrom(candidate)
                if (directoryResult.isSuccess) {
                    clearAddDialogForm()
                    actionState.update {
                        it.copy(
                            addError = null,
                            directoryError = null,
                            lastDirectoryCount = directoryResult.getOrThrow()
                        )
                    }
                    healthMonitor.refresh()
                    return@withLock
                }

                configuration.addManual(candidate).fold(
                    onSuccess = {
                        clearAddDialogForm()
                        actionState.update { it.copy(addError = null) }
                        healthMonitor.refresh()
                    },
                    onFailure = { error ->
                        SparrowLog.error("ControlPlaneSettingsViewModel", "Could not add control plane", error)
                        actionState.update { it.copy(addError = ControlPlaneSettingsError.SAVE_FAILED) }
                    }
                )
            }
        }
    }

    private fun removeControlPlane(url: String) {
        viewModelScope.launch {
            configurationActionMutex.withLock {
                configuration.removeManual(url).fold(
                    onSuccess = {
                        // Update reachability before accepting an immediate re-add.
                        // Do not refresh the external directory here: it can re-import
                        // the entry while the user is removing the manual one.
                        healthMonitor.refresh()
                    },
                    onFailure = { error ->
                        SparrowLog.error("ControlPlaneSettingsViewModel", "Could not remove control plane", error)
                        actionState.update { state ->
                            state.copy(addError = ControlPlaneSettingsError.KEEP_ONE)
                        }
                    }
                )
            }
        }
    }

    private fun saveDirectoryUrl() {
        viewModelScope.launch {
            configurationActionMutex.withLock {
                val draft = directoryDraft.value ?: configuration.directoryUrl.value.orEmpty()
                val normalized = draft.takeIf(String::isNotBlank)?.normalizeHttpUrl()
                if (draft.isNotBlank() && normalized == null) {
                    actionState.update { it.copy(directoryError = ControlPlaneDirectoryError.INVALID_URL) }
                    return@withLock
                }

                configuration.setDirectoryUrl(normalized).fold(
                    onSuccess = {
                        clearDirectoryDraft()
                        refreshAllNow()
                    },
                    onFailure = { error ->
                        SparrowLog.error("ControlPlaneSettingsViewModel", "Could not save directory URL", error)
                        actionState.update { state ->
                            state.copy(directoryError = ControlPlaneDirectoryError.SAVE_FAILED)
                        }
                    }
                )
            }
        }
    }

    private fun clearAddDialogForm() {
        showAddDialog.value = false
        newUrl.value = ""
    }

    private fun clearDirectoryDraft() {
        directoryDraft.value = null
        savedStateHandle.remove<String>(DIRECTORY_DRAFT_KEY)
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
        viewModelScope.launch {
            configurationActionMutex.withLock { refreshAllNow() }
        }
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

        directoryResult.exceptionOrNull()?.let { error ->
            SparrowLog.error("ControlPlaneSettingsViewModel", "Directory synchronization failed", error)
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

    private data class ControlPlaneFormState(
        val showAddDialog: Boolean = false,
        val newUrl: String = "",
        val directoryDraft: String? = null
    )

    private data class ControlPlaneActionState(
        val addError: ControlPlaneSettingsError? = null,
        val directoryError: ControlPlaneDirectoryError? = null,
        val isRefreshing: Boolean = false,
        val isDirectorySyncing: Boolean = false,
        val lastDirectoryCount: Int? = null
    )

    private companion object {
        const val SHOW_ADD_DIALOG_KEY = "showAddDialog"
        const val NEW_URL_KEY = "newUrl"
        const val DIRECTORY_DRAFT_KEY = "directoryDraft"
        const val HEALTH_REFRESH_INTERVAL_MILLISECONDS = 1_000L
    }
}

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
