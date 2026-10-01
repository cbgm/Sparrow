package com.cbgm.sparrow.feature.media.presentation.filepicker.mapper

import com.cbgm.sparrow.feature.media.domain.model.FileBrowserEntry
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FileBrowserEntryUi
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSortModeUi

internal fun List<FileBrowserEntry>.toFilePickerEntriesUi(
    searchQuery: String,
    sortMode: FilePickerSortModeUi,
    sortAscending: Boolean,
    blockedSourceReferences: Set<String>
): List<FileBrowserEntryUi> {
    val query = searchQuery.trim()
    val filtered =
        if (query.isEmpty()) {
            this
        } else {
            filter { entry -> entry.displayName.contains(query, ignoreCase = true) }
        }

    val directories =
        filtered
            .filter(FileBrowserEntry::isDirectory)
            .sortedBy { entry -> entry.displayName.lowercase() }
    val files =
        filtered
            .filterNot(FileBrowserEntry::isDirectory)
            .sortedFilesBy(sortMode)
            .let { sorted -> if (sortAscending) sorted else sorted.asReversed() }

    return (directories + files).map { entry ->
        entry.toFileBrowserEntryUi(blockedSourceReferences)
    }
}

private fun List<FileBrowserEntry>.sortedFilesBy(sortMode: FilePickerSortModeUi): List<FileBrowserEntry> =
    when (sortMode) {
        FilePickerSortModeUi.NAME ->
            sortedBy { entry -> entry.displayName.lowercase() }

        FilePickerSortModeUi.SIZE ->
            sortedWith(
                compareBy<FileBrowserEntry> { entry -> entry.byteSize ?: Long.MAX_VALUE }
                    .thenBy { entry -> entry.displayName.lowercase() }
            )

        FilePickerSortModeUi.TYPE ->
            sortedWith(
                compareBy<FileBrowserEntry> { entry -> entry.mimeType.orEmpty() }
                    .thenBy { entry -> entry.displayName.lowercase() }
            )
    }
