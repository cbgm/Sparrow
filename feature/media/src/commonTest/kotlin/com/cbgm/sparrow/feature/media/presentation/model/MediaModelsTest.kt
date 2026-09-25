package com.cbgm.sparrow.feature.media.presentation.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class MediaModelsTest {
    @Test
    fun selectionsAreComparedUsingFileMetadataInsteadOfBinaryData() {
        val first = MediaSelection(
            id = "selection-a",
            type = MediaSelectionType.IMAGE,
            localFilePath = "/private/pending/a.content",
            byteSize = 128,
            mimeType = "image/png",
            source = MediaSelectionSource.GALLERY,
            thumbnailFilePath = "/private/pending/a.preview"
        )
        assertEquals(first, first.copy())
        assertNotEquals(first, first.copy(localFilePath = "/private/pending/b.content"))
        assertEquals(128L, first.byteSize)
    }

    @Test
    fun mediaViewerItemsReferenceFilesOnly() {
        val item = MediaItem(
            id = "media-a",
            type = MediaType.VIDEO,
            mimeType = "video/mp4",
            localFilePath = "/private/pending/a.content",
            thumbnailFilePath = "/private/pending/a.preview"
        )
        assertEquals("/private/pending/a.content", item.localFilePath)
        assertEquals("/private/pending/a.preview", item.thumbnailFilePath)
    }
}
