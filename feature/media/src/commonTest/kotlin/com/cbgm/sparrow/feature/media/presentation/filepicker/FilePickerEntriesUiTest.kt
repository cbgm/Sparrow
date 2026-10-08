package com.cbgm.sparrow.feature.media.presentation.filepicker

import com.cbgm.sparrow.feature.media.domain.model.FileBrowserEntry
import com.cbgm.sparrow.feature.media.presentation.filepicker.mapper.toFilePickerEntriesUi
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSortModeUi
import kotlin.test.Test
import kotlin.test.assertEquals

class FilePickerEntriesUiTest {
    @Test
    fun directoriesStayBeforeFilesAndFileDirectionChanges() {
        val entries =
            listOf(
                entry("file-b", "b.txt", size = 2, mimeType = "text/plain"),
                entry("dir-b", "Beta", isDirectory = true),
                entry("file-a", "a.txt", size = 1, mimeType = "text/plain"),
                entry("dir-a", "Alpha", isDirectory = true)
            )

        val ascending =
            entries.toFilePickerEntriesUi(
                searchQuery = "",
                sortMode = FilePickerSortModeUi.NAME,
                sortAscending = true,
                blockedSourceReferences = emptySet()
            )
        assertEquals(listOf("dir-a", "dir-b", "file-a", "file-b"), ascending.map { it.reference })

        val descending =
            entries.toFilePickerEntriesUi(
                searchQuery = "",
                sortMode = FilePickerSortModeUi.NAME,
                sortAscending = false,
                blockedSourceReferences = emptySet()
            )
        assertEquals(listOf("dir-a", "dir-b", "file-b", "file-a"), descending.map { it.reference })
    }

    @Test
    fun searchAndSizeSortAreAppliedBeforeMapping() {
        val entries =
            listOf(
                entry("large", "Report-large.pdf", size = 20, mimeType = "application/pdf"),
                entry("small", "Report-small.pdf", size = 10, mimeType = "application/pdf"),
                entry("other", "notes.txt", size = 1, mimeType = "text/plain")
            )

        val result =
            entries.toFilePickerEntriesUi(
                searchQuery = " report ",
                sortMode = FilePickerSortModeUi.SIZE,
                sortAscending = true,
                blockedSourceReferences = emptySet()
            )

        assertEquals(listOf("small", "large"), result.map { it.reference })
    }

    private fun entry(
        reference: String,
        displayName: String,
        isDirectory: Boolean = false,
        size: Long? = null,
        mimeType: String? = null
    ) =
        FileBrowserEntry(
            reference = reference,
            sourceReference = reference,
            displayName = displayName,
            isDirectory = isDirectory,
            byteSize = size,
            mimeType = mimeType
        )
}
