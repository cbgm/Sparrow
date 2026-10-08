package com.cbgm.sparrow.feature.attachments.presentation.mapper

import com.cbgm.sparrow.core.messagepart.ui.model.ImageUi
import com.cbgm.sparrow.core.messagepart.ui.model.VideoUi
import kotlin.test.Test
import kotlin.test.assertEquals

class MessagePartMediaMapperTest {
    @Test
    fun imageKeepsLocalPaths() {
        val media =
            ImageUi(
                id = "image-1",
                mimeType = "image/jpeg",
                byteSize = 42L,
                localFilePath = "/cache/image.jpg",
                thumbnailFilePath = "/cache/image-thumb.jpg"
            ).toMediaItemUi()

        assertEquals("/cache/image.jpg", media.localFilePath)
        assertEquals("/cache/image-thumb.jpg", media.thumbnailFilePath)
    }

    @Test
    fun videoKeepsLocalPaths() {
        val media =
            VideoUi(
                id = "video-1",
                mimeType = "video/mp4",
                byteSize = 42L,
                localFilePath = "/cache/video.mp4",
                thumbnailFilePath = "/cache/video-thumb.jpg"
            ).toMediaItemUi()

        assertEquals("/cache/video.mp4", media.localFilePath)
        assertEquals("/cache/video-thumb.jpg", media.thumbnailFilePath)
    }
}
