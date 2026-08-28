package com.cbgm.sparrow.feature.media.presentation.filepicker.model

sealed interface FilePickerUiEvent {
    data object BackClicked : FilePickerUiEvent

    data object CloseClicked : FilePickerUiEvent

    data object GrantFileAccessClicked : FilePickerUiEvent

    data class FileAccessReturned(
        val rootReference: String?
    ) : FilePickerUiEvent

    data class FileAccessError(
        val message: String
    ) : FilePickerUiEvent

    data class EntryClicked(
        val reference: String
    ) : FilePickerUiEvent

    data class BreadcrumbClicked(
        val reference: String
    ) : FilePickerUiEvent

    data class SearchChanged(
        val query: String
    ) : FilePickerUiEvent

    data object SearchCleared : FilePickerUiEvent

    data class SortSelected(
        val mode: FilePickerSortMode
    ) : FilePickerUiEvent

    data object SortDirectionToggled : FilePickerUiEvent

    data object ConfirmClicked : FilePickerUiEvent
}
