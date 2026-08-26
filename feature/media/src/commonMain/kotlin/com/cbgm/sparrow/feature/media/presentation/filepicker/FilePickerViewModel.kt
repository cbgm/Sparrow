package com.cbgm.sparrow.feature.media.presentation.filepicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserDirectory
import com.cbgm.sparrow.feature.media.domain.model.FileBrowserEntry
import com.cbgm.sparrow.feature.media.domain.usecase.BrowseFileDirectoryUseCase
import com.cbgm.sparrow.feature.media.domain.usecase.CheckFileBrowserAccessUseCase
import com.cbgm.sparrow.feature.media.domain.usecase.GetFileBrowserRootUseCase
import com.cbgm.sparrow.feature.media.domain.usecase.ReadFileBrowserEntryUseCase
import com.cbgm.sparrow.feature.media.domain.usecase.SetFileBrowserRootUseCase
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerBreadcrumbUi
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSortMode
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerUiEvent
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerUiState
import com.cbgm.sparrow.feature.media.presentation.mapper.toAttachmentSelection
import com.cbgm.sparrow.feature.media.presentation.mapper.toUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FilePickerViewModel(
    savedStateHandle: SavedStateHandle,
    private val sessions: FilePickerSessionController,
    private val checkAccess: CheckFileBrowserAccessUseCase,
    private val setRoot: SetFileBrowserRootUseCase,
    private val getRoot: GetFileBrowserRootUseCase,
    private val browseDirectory: BrowseFileDirectoryUseCase,
    private val readFile: ReadFileBrowserEntryUseCase
) : BaseViewModel() {
    private val sessionId = savedStateHandle.requireRouteArgument<String>(AppRoute.FilePicker::sessionId.name)
    private val session = sessions.snapshot(sessionId)
    private val blockedSourceReferences = session?.blockedSourceReferences.orEmpty()
    private val directoryStack = mutableListOf<DirectoryState>()
    private var rawEntries: List<FileBrowserEntry> = emptyList()
    private var entriesByReference: Map<String, FileBrowserEntry> = emptyMap()

    private val _uiState =
        MutableStateFlow(
            FilePickerUiState(
                selectionCapacity = session?.maxItems ?: 0,
                errorMessage = if (session == null) "File picker session is no longer available" else null
            )
        )
    val uiState: StateFlow<FilePickerUiState> = _uiState.asStateFlow()

    init {
        if (session != null) refreshAccessAndOpenRoot()
    }

    fun onExternalError(message: String) {
        sessions.reportError(sessionId, message)
        _uiState.update { state -> state.copy(errorMessage = message, isLoading = false) }
    }

    fun dismissSessionIfActive() {
        if (sessions.isActive(sessionId)) sessions.dismiss(sessionId)
    }

    fun onUiEvent(event: FilePickerUiEvent) {
        when (event) {
            FilePickerUiEvent.BackClicked -> handleBack()
            FilePickerUiEvent.CloseClicked -> closePicker()
            is FilePickerUiEvent.FileAccessReturned -> handleFileAccessReturned(event.rootReference)
            is FilePickerUiEvent.EntryClicked -> handleEntryClick(event.reference)
            is FilePickerUiEvent.BreadcrumbClicked -> navigateToBreadcrumb(event.reference)
            is FilePickerUiEvent.SearchChanged -> updateSearch(event.query)
            FilePickerUiEvent.SearchCleared -> updateSearch("")
            is FilePickerUiEvent.SortSelected -> updateSort(event.mode)
            FilePickerUiEvent.SortDirectionToggled -> toggleSortDirection()
            FilePickerUiEvent.ConfirmClicked -> confirmSelection()
        }
    }

    private fun handleFileAccessReturned(rootReference: String?) {
        if (rootReference == null) {
            refreshAccessAndOpenRoot()
            return
        }

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true, errorMessage = null) }
            setRoot(rootReference)
                .onSuccess { refreshAccessAndOpenRoot() }
                .onFailure(::showError)
        }
    }

    private fun refreshAccessAndOpenRoot() {
        if (!checkAccess()) {
            _uiState.update { state ->
                state.copy(
                    requiresFileAccess = true,
                    isLoading = false,
                    errorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state -> state.copy(requiresFileAccess = false, isLoading = true, errorMessage = null) }
            getRoot()
                .onSuccess(::openRoot)
                .onFailure(::showError)
        }
    }

    private fun openRoot(root: FileBrowserDirectory) {
        directoryStack.clear()
        directoryStack += DirectoryState(root.reference, root.displayName)
        loadDirectory(root.reference)
    }

    private fun handleBack() {
        if (directoryStack.size <= 1) {
            sessions.dismiss(sessionId)
            navigator.popBackStack()
            return
        }

        directoryStack.removeLast()
        loadDirectory(directoryStack.last().reference)
    }

    private fun closePicker() {
        sessions.dismiss(sessionId)
        navigator.popBackStack()
    }

    private fun navigateToBreadcrumb(reference: String) {
        val targetIndex = directoryStack.indexOfFirst { it.reference == reference }
        if (targetIndex < 0 || targetIndex == directoryStack.lastIndex) return
        while (directoryStack.lastIndex > targetIndex) directoryStack.removeLast()
        loadDirectory(reference)
    }

    private fun handleEntryClick(reference: String) {
        val entry = entriesByReference[reference] ?: return
        if (entry.isDirectory) {
            directoryStack += DirectoryState(entry.reference, entry.displayName)
            loadDirectory(entry.reference)
        } else {
            toggleFile(entry)
        }
    }

    private fun loadDirectory(reference: String) {
        viewModelScope.launch {
            setLoading()
            browseDirectory(reference)
                .onSuccess { entries ->
                    rawEntries = entries
                    entriesByReference = entries.associateBy(FileBrowserEntry::reference)
                    _uiState.update { state ->
                        state.copy(
                            currentDirectoryName = directoryStack.lastOrNull()?.displayName ?: "Files",
                            breadcrumbs =
                                directoryStack.map { directory ->
                                    FilePickerBreadcrumbUi(
                                        reference = directory.reference,
                                        displayName = directory.displayName
                                    )
                                },
                            searchQuery = "",
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    publishEntries()
                }
                .onFailure(::showError)
        }
    }

    private fun toggleFile(entry: FileBrowserEntry) {
        if (entry.sourceReference != null && entry.sourceReference in blockedSourceReferences) return

        _uiState.update { state ->
            val selected = state.selectedReferences.toMutableSet()
            if (!selected.add(entry.reference)) {
                selected.remove(entry.reference)
            } else if (selected.size > state.selectionCapacity) {
                return@update state.copy(errorMessage = "You can select at most ${state.selectionCapacity} file(s)")
            }
            state.copy(
                selectedReferences = selected,
                selectedCount = selected.size,
                errorMessage = null
            )
        }
        publishEntries()
    }

    private fun updateSearch(query: String) {
        _uiState.update { state -> state.copy(searchQuery = query, errorMessage = null) }
        publishEntries()
    }

    private fun updateSort(mode: FilePickerSortMode) {
        _uiState.update { state -> state.copy(sortMode = mode) }
        publishEntries()
    }

    private fun toggleSortDirection() {
        _uiState.update { state -> state.copy(sortAscending = !state.sortAscending) }
        publishEntries()
    }

    private fun publishEntries() {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        val filtered =
            if (query.isEmpty()) rawEntries else rawEntries.filter { it.displayName.contains(query, ignoreCase = true) }
        val directories = filtered.filter(FileBrowserEntry::isDirectory).sortedBy { it.displayName.lowercase() }
        val files = filtered.filterNot(FileBrowserEntry::isDirectory)
        val sortedFiles =
            when (state.sortMode) {
                FilePickerSortMode.NAME -> files.sortedBy { it.displayName.lowercase() }
                FilePickerSortMode.SIZE ->
                    files.sortedWith(
                        compareBy<FileBrowserEntry> { it.byteSize ?: Long.MAX_VALUE }
                            .thenBy { it.displayName.lowercase() }
                    )

                FilePickerSortMode.TYPE ->
                    files.sortedWith(
                        compareBy<FileBrowserEntry> { it.mimeType.orEmpty() }
                            .thenBy { it.displayName.lowercase() }
                    )
            }.let { sorted -> if (state.sortAscending) sorted else sorted.reversed() }

        _uiState.update { current ->
            current.copy(entries = (directories + sortedFiles).map { it.toUiModel(blockedSourceReferences) })
        }
    }

    private fun confirmSelection() {
        val currentSession = session ?: return
        val selectedReferences = _uiState.value.selectedReferences.toList()
        if (selectedReferences.isEmpty() || _uiState.value.isConfirming) return

        _uiState.update { state -> state.copy(isConfirming = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                selectedReferences.map { reference ->
                    readFile(reference, currentSession.maxFileBytes).getOrThrow().toAttachmentSelection()
                }
            }.onSuccess { files ->
                sessions.complete(sessionId, files)
                navigator.popBackStack()
            }.onFailure { error ->
                _uiState.update { state -> state.copy(isConfirming = false) }
                showError(error)
            }
        }
    }

    private fun setLoading() {
        _uiState.update { state -> state.copy(isLoading = true, errorMessage = null) }
    }

    private fun showError(error: Throwable) {
        val message = error.message ?: "Files could not be loaded"
        sessions.reportError(sessionId, message)
        _uiState.update { state -> state.copy(isLoading = false, isConfirming = false, errorMessage = message) }
    }
}

private data class DirectoryState(
    val reference: String,
    val displayName: String
)
