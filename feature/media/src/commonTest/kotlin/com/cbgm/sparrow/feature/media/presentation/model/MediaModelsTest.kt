package com.cbgm.sparrow.feature.media.presentation.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MediaModelsTest {
    @Test
    fun visualSelectionCarriesVisualMetadata() {
        val selection: MediaSelectionUi =
            VisualMediaSelectionUi(
                id = "image-1",
                localFilePath = "/media/image.jpg",
                byteSize = 42,
                mimeType = "image/jpeg",
                source = MediaSourceUi.GALLERY,
                type = MediaTypeUi.IMAGE,
                width = 100,
                height = 80
            )

        val visual = assertIs<VisualMediaSelectionUi>(selection)
        assertEquals(MediaTypeUi.IMAGE, visual.type)
        assertEquals(100, visual.width)
        assertEquals(80, visual.height)
    }

    @Test
    fun mediaItemUsesTypeInsteadOfSubtype() {
        val item =
            MediaItemUi(
                id = "video-1",
                type = MediaTypeUi.VIDEO,
                mimeType = "video/mp4",
                durationMilliseconds = 1_234
            )

        assertEquals(MediaTypeUi.VIDEO, item.type)
        assertEquals(1_234L, item.durationMilliseconds)
    }
}
