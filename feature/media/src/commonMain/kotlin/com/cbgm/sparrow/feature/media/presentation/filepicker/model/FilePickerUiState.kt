package com.cbgm.sparrow.feature.media.presentation.filepicker.model

data class FilePickerUiState(
    val currentDirectoryName: String = "Files",
    val breadcrumbs: List<FilePickerBreadcrumbUi> = emptyList(),
    val entries: List<FileBrowserEntryUi> = emptyList(),
    val searchQuery: String = "",
    val sortMode: FilePickerSortMode = FilePickerSortMode.NAME,
    val sortAscending: Boolean = true,
    val selectedReferences: Set<String> = emptySet(),
    val selectedCount: Int = 0,
    val selectionCapacity: Int = 0,
    val requiresFileAccess: Boolean = false,
    val isLoading: Boolean = true,
    val isConfirming: Boolean = false,
    val errorMessage: String? = null
) {
    val canConfirm: Boolean
        get() = selectedCount > 0 && !isLoading && !isConfirming
}

data class FilePickerBreadcrumbUi(
    val reference: String,
    val displayName: String
)

data class FileBrowserEntryUi(
    val reference: String,
    val displayName: String,
    val kind: FileBrowserEntryKind,
    val sizeText: String?,
    val typeText: String?,
    val isBlocked: Boolean
) {
    val isDirectory: Boolean
        get() = kind == FileBrowserEntryKind.DIRECTORY
}

enum class FileBrowserEntryKind {
    DIRECTORY,
    IMAGE,
    VIDEO,
    AUDIO,
    PDF,
    TEXT,
    ARCHIVE,
    OTHER
}

enum class FilePickerSortMode {
    NAME,
    SIZE,
    TYPE
}
