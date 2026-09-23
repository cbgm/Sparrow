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
import com.cbgm.sparrow.feature.settings.presentation.network.model.ControlPlaneAddSource
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
    private val addSource = savedStateHandle.getMutableStateFlow(
        ADD_SOURCE_KEY,
        ControlPlaneAddSource.PLANE
    )
    private val directoryDraft =
        MutableStateFlow(savedStateHandle.get<String>(DIRECTORY_DRAFT_KEY))
    private val actionState = MutableStateFlow(ControlPlaneActionState())

    // Serialize remove, add and directory refresh. A user may tap Add immediately
    // after Remove; those operations must not race the automatic refresh.
    private val configurationActionMutex = Mutex()
    private val formState =
        combine(showAddDialog, newUrl, directoryDraft, addSource) { showAddDialog, newUrl, directoryDraft, addSource ->
            ControlPlaneFormState(
                showAddDialog = showAddDialog,
                newUrl = newUrl,
                directoryDraft = directoryDraft,
                addSource = addSource
            )
        }

    private val configurationContext = observeControlPlaneSettingsContext()

    val uiState: StateFlow<ControlPlaneSettingsUiState> =
        combine(
            configurationContext,
            formState,
            actionState,
            this.configuration.jsonDirectoryUrl,
            this.configuration.jsonDirectoryBaseUrls
        ) { configuration, form, action, jsonDirectoryUrl, jsonDirectoryBaseUrls ->
            configuration.statuses
                .toControlPlaneUiModels(configuration.manualBaseUrls, configuration.directoryBaseUrls, jsonDirectoryBaseUrls)
                .toControlPlaneSettingsUiState(
                    showAddDialog = form.showAddDialog,
                    isEditingDirectory = action.editingDirectory,
                    newUrl = form.newUrl,
                    addSource = form.addSource,
                    jsonDirectoryUrl = jsonDirectoryUrl.orEmpty(),
                    addError = action.addError,
                    directoryUrl = configuration.directoryUrl,
                    directoryDraft = form.directoryDraft ?: configuration.directoryUrl,
                    directoryError = action.directoryError,
                    directoryFailureDetail = action.directoryFailureDetail,
                    isRefreshing = action.isRefreshing,
                    isDirectorySyncing = action.isDirectorySyncing,
                    lastDirectoryCount = action.lastDirectoryCount,
                    manualRemovalError = action.manualRemovalError
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
            ControlPlaneSettingsUiEvent.EditDirectoryClicked -> openDirectoryEditor()
            ControlPlaneSettingsUiEvent.AddDismissed -> closeAddDialog()
            ControlPlaneSettingsUiEvent.AddConfirmed -> addControlPlane()
            is ControlPlaneSettingsUiEvent.AddSourceChanged -> addSource.value = event.source
            ControlPlaneSettingsUiEvent.ClearJsonDirectory -> clearJsonDirectory()
            ControlPlaneSettingsUiEvent.DirectoryApply -> saveDirectoryUrl()
            ControlPlaneSettingsUiEvent.Refresh -> refreshAll()
            is ControlPlaneSettingsUiEvent.NewUrlChanged -> updateNewUrl(event.value)
            is ControlPlaneSettingsUiEvent.Remove -> removeControlPlane(event.url)
            ControlPlaneSettingsUiEvent.RemoveDirectory -> removeDirectory()
        }
    }

    private fun openAddDialog() {
        showAddDialog.value = true
        newUrl.value = ""
        addSource.value = ControlPlaneAddSource.PLANE
        actionState.update { state -> state.copy(addError = null, editingDirectory = false) }
    }

    private fun openDirectoryEditor() {
        // There is one editable directory source (signed OR legacy JSON).
        // Individual manually-added Control Planes stay independent.
        val signed = configuration.directoryUrl.value
        addSource.value = if (signed != null || configuration.jsonDirectoryUrl.value == null) {
            ControlPlaneAddSource.SIGNED_DIRECTORY
        } else {
            ControlPlaneAddSource.JSON_LIST
        }
        newUrl.value = signed ?: configuration.jsonDirectoryUrl.value.orEmpty()
        showAddDialog.value = true
        actionState.update { it.copy(addError = null, editingDirectory = true) }
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
                val raw = newUrl.value.trim()
                val type = addSource.value
                val candidate = when (type) {
                    ControlPlaneAddSource.PLANE -> raw.normalizePlaneOrigin()
                    ControlPlaneAddSource.SIGNED_DIRECTORY -> raw.normalizeDirectoryOrigin()
                    ControlPlaneAddSource.JSON_LIST -> raw.normalizeJsonListUrl()
                }
                if (actionState.value.editingDirectory && type == ControlPlaneAddSource.PLANE) {
                    actionState.update { it.copy(addError = ControlPlaneSettingsError.INVALID_URL) }
                    return@withLock
                }
                if (candidate == null) {
                    actionState.update { it.copy(addError = ControlPlaneSettingsError.INVALID_URL) }
                    return@withLock
                }
                if (type == ControlPlaneAddSource.PLANE &&
                    (
                        candidate == configuration.directoryUrl.value ||
                            candidate == configuration.jsonDirectoryUrl.value
                    )
                ) {
                    actionState.update { it.copy(addError = ControlPlaneSettingsError.INVALID_URL) }
                    return@withLock
                }
                if (type == ControlPlaneAddSource.PLANE && candidate in configuration.manualBaseUrls.value) {
                    actionState.update { it.copy(addError = ControlPlaneSettingsError.DUPLICATE) }
                    return@withLock
                }
                when (type) {
                    ControlPlaneAddSource.PLANE -> {
                        configuration.addManual(candidate).fold(
                            onSuccess = {
                                clearAddDialogForm()
                                actionState.update { it.copy(addError = null) }
                                healthMonitor.refresh()
                            },
                            onFailure = { error ->
                                SparrowLog.withTag("ControlPlaneSettingsViewModel").warn {
                                    "Manual Control Plane was not saved: ${error.message}"
                                }
                                actionState.update { it.copy(addError = ControlPlaneSettingsError.SAVE_FAILED) }
                            }
                        )
                    }
                    ControlPlaneAddSource.SIGNED_DIRECTORY -> {
                        // The first directory is a *setting*, not a successful HTTP request.
                        // Persist it so offline/unreachable directories can be edited or removed.
                        // A replacement URL may not change an existing trusted signer unless
                        // its signed snapshot has actually been verified first.
                        val previouslyConfigured = configuration.directoryUrl.value
                        val syncResult = if (previouslyConfigured != null &&
                            previouslyConfigured != candidate
                        ) {
                            directorySynchronizer.synchronizeFrom(candidate)
                        } else {
                            configuration.setDirectoryUrl(candidate).map { 0 }
                        }
                        syncResult.fold(
                            onSuccess = {
                                configuration.clearJsonDirectory().fold(
                                    onSuccess = {
                                        clearAddDialogForm()
                                        actionState.update { it.copy(addError = null, editingDirectory = false) }
                                        refreshDirectoryWithoutGlobalError()
                                    },
                                    onFailure = { error -> showDirectorySaveError(error) }
                                )
                            },
                            onFailure = { error -> showDirectorySaveError(error) }
                        )
                    }
                    ControlPlaneAddSource.JSON_LIST -> {
                        // A JSON URL is a one-time bulk manual import, not the
                        // signed directory setting and not a recurring feed.
                        directorySynchronizer.importJsonDirectory(candidate).fold(
                            onSuccess = {
                                clearAddDialogForm()
                                actionState.update { it.copy(addError = null, editingDirectory = false) }
                                healthMonitor.refresh()
                            },
                            onFailure = { error -> showDirectorySaveError(error) }
                        )
                    }
                }
            }
        }
    }

    private fun clearJsonDirectory() {
        viewModelScope.launch {
            configurationActionMutex.withLock {
                configuration.clearJsonDirectory().onSuccess { healthMonitor.refresh() }
            }
        }
    }

    private fun removeControlPlane(url: String) {
        viewModelScope.launch {
            configurationActionMutex.withLock {
                configuration.removeManual(url).fold(
                    onSuccess = {
                        actionState.update { it.copy(manualRemovalError = null) }
                        // Update reachability before accepting an immediate re-add.
                        // Do not refresh the external directory here: it can re-import
                        // the entry while the user is removing the manual one.
                        healthMonitor.refresh()
                    },
                    onFailure = { error ->
                        SparrowLog.error("ControlPlaneSettingsViewModel", "Could not remove control plane", error)
                        actionState.update { state ->
                            state.copy(manualRemovalError = error.message ?: "Could not remove Control Plane")
                        }
                    }
                )
            }
        }
    }

    private fun saveDirectoryUrl() = addControlPlane()

    private fun showDirectorySaveError(error: Throwable) {
        SparrowLog.withTag("ControlPlaneSettingsViewModel").warn {
            "Directory configuration could not be saved: ${error.message}"
        }
        actionState.update {
            it.copy(
                addError = ControlPlaneSettingsError.SAVE_FAILED,
                directoryFailureDetail = error.message?.take(180)
            )
        }
    }

    private suspend fun refreshDirectoryWithoutGlobalError() {
        actionState.update {
            it.copy(isDirectorySyncing = true, directoryError = null, directoryFailureDetail = null)
        }
        // The directory remains configured even when the endpoint is currently offline.
        val refreshResult = directorySynchronizer.refresh()
        actionState.update {
            it.copy(
                isDirectorySyncing = false,
                directoryError = if (refreshResult.isFailure) ControlPlaneDirectoryError.SYNC_FAILED else null,
                directoryFailureDetail = refreshResult.exceptionOrNull()?.message?.take(180),
                lastDirectoryCount = refreshResult.getOrNull() ?: it.lastDirectoryCount
            )
        }
        refreshResult.exceptionOrNull()?.let { error ->
            SparrowLog.withTag("ControlPlaneSettingsViewModel").warn {
                "Configured directory unavailable (cached planes retained): ${error.message}"
            }
        }
        healthMonitor.refresh()
    }

    private fun removeDirectory() {
        viewModelScope.launch {
            configurationActionMutex.withLock {
                // Only an explicit removal discards discovery caches and signing
                // trust. Editing the URL/source must never invoke this path.
                configuration.clearJsonDirectory().fold(
                    onSuccess = {
                        configuration.clearJsonDirectoryCache().fold(
                            onSuccess = {
                                configuration.setDirectoryUrl(null).fold(
                                    onSuccess = {
                                        directorySynchronizer.forgetDirectoryTrustOnUserRemoval().fold(
                                            onSuccess = {
                                                clearDirectoryDraft()
                                                actionState.update {
                                                    it.copy(
                                                        directoryError = null,
                                                        directoryFailureDetail = null,
                                                        addError = null,
                                                        lastDirectoryCount = 0
                                                    )
                                                }
                                                healthMonitor.refresh()
                                            },
                                            onFailure = ::showDirectorySaveError
                                        )
                                    },
                                    onFailure = ::showDirectorySaveError
                                )
                            },
                            onFailure = ::showDirectorySaveError
                        )
                    },
                    onFailure = ::showDirectorySaveError
                )
            }
        }
    }

    private fun clearAddDialogForm() {
        showAddDialog.value = false
        newUrl.value = ""
        addSource.value = ControlPlaneAddSource.PLANE
        actionState.update { it.copy(editingDirectory = false) }
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
                directoryError = null,
                directoryFailureDetail = null
            )
        }

        val directoryResult =
            if (configuration.directoryUrl.value == null && configuration.jsonDirectoryUrl.value == null) {
                Result.success(0)
            } else {
                actionState.update { it.copy(isDirectorySyncing = true) }
                directorySynchronizer.refresh()
            }

        directoryResult.exceptionOrNull()?.let { error ->
            // Directory availability is optional; the verified cache remains
            // usable. Surface this in the directory settings status instead
            // of emitting an application-wide failure snackbar.
            SparrowLog.withTag("ControlPlaneSettingsViewModel").warn {
                "Directory synchronization unavailable/rejected: ${error.message}"
            }
        }
        healthMonitor.refresh()
        actionState.update { current ->
            current.copy(
                isRefreshing = false,
                isDirectorySyncing = false,
                lastDirectoryCount = directoryResult.getOrNull() ?: current.lastDirectoryCount,
                directoryError =
                    if (directoryResult.isFailure) ControlPlaneDirectoryError.SYNC_FAILED else null,
                directoryFailureDetail = directoryResult.exceptionOrNull()?.message?.take(180)
            )
        }
    }

    private data class ControlPlaneFormState(
        val showAddDialog: Boolean = false,
        val newUrl: String = "",
        val addSource: ControlPlaneAddSource = ControlPlaneAddSource.PLANE,
        val directoryDraft: String? = null
    )

    private data class ControlPlaneActionState(
        val editingDirectory: Boolean = false,
        val addError: ControlPlaneSettingsError? = null,
        val directoryError: ControlPlaneDirectoryError? = null,
        val isRefreshing: Boolean = false,
        val isDirectorySyncing: Boolean = false,
        val lastDirectoryCount: Int? = null,
        val directoryFailureDetail: String? = null,
        val manualRemovalError: String? = null
    )

    private companion object {
        const val SHOW_ADD_DIALOG_KEY = "showAddDialog"
        const val NEW_URL_KEY = "newUrl"
        const val ADD_SOURCE_KEY = "addSource"
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

private fun String.normalizeDirectoryOrigin(): String? {
    val candidate = normalizeHttpUrl() ?: return null
    if (!candidate.startsWith("https://") || candidate.length > 253) return null
    val host = candidate.removePrefix("https://")
    if (host.count { it == '.' } < 1 || host.split('.').any { label ->
            label.isEmpty() || label.length > 63 || !label.first().isLetterOrDigit() ||
                !label.last().isLetterOrDigit() || label.any {
                    it !in 'a'..'z' && it !in 'A'..'Z' && it !in '0'..'9' && it != '-'
                }
        }
    ) {
        return null
    }
    return "https://${host.lowercase()}"
}

/** A plane URL must be an origin, never the central directory or a JSON document path. */
private fun String.normalizePlaneOrigin(): String? {
    val candidate = normalizeHttpUrl() ?: return null
    return candidate.takeIf {
        Regex("^https?://[A-Za-z0-9.-]+(?::[0-9]{1,5})?$").matches(it)
    }
}

private fun String.normalizeJsonListUrl(): String? {
    val candidate = trim()
    return candidate.takeIf { value ->
        value.length <= 2048 &&
            Regex("^https?://[^\\s/@?#]+(?:/[^\\s?#]*)?(?:\\?[^\\s#]*)?$")
                .matches(value) && !value.contains('@')
    }
}
