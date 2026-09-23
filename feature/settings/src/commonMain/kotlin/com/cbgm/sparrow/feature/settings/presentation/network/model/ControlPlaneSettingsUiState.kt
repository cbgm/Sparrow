package com.cbgm.sparrow.feature.settings.presentation.network.model

data class ControlPlaneSettingsUiState(
    val entries: List<ControlPlaneUiModel> = emptyList(),
    val showAddDialog: Boolean = false,
    val isEditingDirectory: Boolean = false,
    val newUrl: String = "",
    val addSource: ControlPlaneAddSource = ControlPlaneAddSource.PLANE,
    val jsonDirectoryUrl: String = "",
    val addError: ControlPlaneSettingsError? = null,
    val directoryUrl: String = "",
    val directoryDraft: String = "",
    val directoryError: ControlPlaneDirectoryError? = null,
    val directoryFailureDetail: String? = null,
    val isRefreshing: Boolean = false,
    val isDirectorySyncing: Boolean = false,
    val lastDirectoryCount: Int? = null,
    val manualRemovalError: String? = null
) {
    val availableCount: Int
        get() =
            entries.count { entry ->
                entry.status == ControlPlaneUiStatus.ACTIVE ||
                    entry.status == ControlPlaneUiStatus.AVAILABLE
            }

    val unavailableCount: Int
        get() = entries.count { it.status == ControlPlaneUiStatus.UNREACHABLE }
}

data class ControlPlaneUiModel(
    val url: String,
    val status: ControlPlaneUiStatus,
    val source: ControlPlaneUiSource,
    val canRemove: Boolean
)

enum class ControlPlaneUiStatus {
    ACTIVE,
    AVAILABLE,
    UNREACHABLE,
    CHECKING
}

enum class ControlPlaneAddSource {
    PLANE,
    SIGNED_DIRECTORY,
    JSON_LIST
}

enum class ControlPlaneUiSource {
    MANUAL,
    DIRECTORY,
    MANUAL_AND_DIRECTORY,
    JSON_LIST,
    MANUAL_AND_JSON_LIST,
    NODE_HINT
}

enum class ControlPlaneSettingsError {
    INVALID_URL,
    DUPLICATE,
    KEEP_ONE,
    SAVE_FAILED
}

enum class ControlPlaneDirectoryError {
    SYNC_FAILED
}

sealed interface ControlPlaneSettingsUiEvent {
    data object BackClicked : ControlPlaneSettingsUiEvent

    data object EditDirectoryClicked : ControlPlaneSettingsUiEvent

    data object AddClicked : ControlPlaneSettingsUiEvent

    data object AddDismissed : ControlPlaneSettingsUiEvent

    data object AddConfirmed : ControlPlaneSettingsUiEvent

    data object Refresh : ControlPlaneSettingsUiEvent

    data object DirectoryApply : ControlPlaneSettingsUiEvent

    data class AddSourceChanged(
        val source: ControlPlaneAddSource
    ) : ControlPlaneSettingsUiEvent

    data object ClearJsonDirectory : ControlPlaneSettingsUiEvent

    data object RemoveDirectory : ControlPlaneSettingsUiEvent

    data class NewUrlChanged(
        val value: String
    ) : ControlPlaneSettingsUiEvent

    data class Remove(
        val url: String
    ) : ControlPlaneSettingsUiEvent
}
