package com.cbgm.sparrow.feature.media.presentation.mapper

import com.cbgm.sparrow.feature.media.domain.model.MediaContentType
import com.cbgm.sparrow.feature.media.domain.model.MediaSource
import com.cbgm.sparrow.feature.media.domain.model.VisualMediaSelection
import com.cbgm.sparrow.feature.media.presentation.model.MediaSourceUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MediaUiMapperTest {
    @Test
    fun visualSelectionMapsToPresentationAndBack() {
        val domain =
            VisualMediaSelection(
                id = "video-1",
                localFilePath = "/media/video.mp4",
                byteSize = 100,
                mimeType = "video/mp4",
                source = MediaSource.GALLERY,
                sourceReference = "gallery-1",
                type = MediaContentType.VIDEO,
                thumbnailFilePath = "/media/video.jpg",
                width = 1920,
                height = 1080,
                durationMilliseconds = 1_000
            )

        val ui = assertIs<VisualMediaSelectionUi>(domain.toUi())
        assertEquals(MediaSourceUi.GALLERY, ui.source)
        assertEquals(MediaTypeUi.VIDEO, ui.type)
        assertEquals(domain, ui.toDomain())
    }
}
