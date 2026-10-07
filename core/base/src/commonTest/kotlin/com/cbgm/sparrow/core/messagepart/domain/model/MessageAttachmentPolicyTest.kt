package com.cbgm.sparrow.core.messagepart.domain.model

import kotlin.test.Test
import kotlin.test.assertFailsWith

class MessageAttachmentPolicyTest {
    @Test
    fun `allows up to eight mixed attachments`() {
        val items = listOf(image(0), video(), image(2)) + List(5) { index -> file(index) }
        MessageAttachmentPolicy.requireValid(items)
    }

    @Test
    fun `rejects more than eight mixed attachments`() {
        val items = List(4) { index -> image(index) } + List(5) { index -> file(index) }
        assertFailsWith<IllegalArgumentException> {
            MessageAttachmentPolicy.requireValid(items)
        }
    }

    @Test
    fun `rejects duplicate ids across media and files`() {
        val items = listOf(image(1, id = "same-id"), file(1, id = "same-id"))
        assertFailsWith<IllegalArgumentException> {
            MessageAttachmentPolicy.requireValid(items)
        }
    }

    @Test
    fun `video requires video mime type`() {
        val part =
            Video(
                id = "video-1",
                mimeType = "image/jpeg",
                byteSize = 1L
            )
        assertFailsWith<IllegalArgumentException> {
            MessageAttachmentPolicy.requireValid(listOf(part))
        }
    }

    private fun image(index: Int, id: String = "image-$index"): MessagePart =
        Image(
            id = id,
            mimeType = "image/jpeg",
            byteSize = 1L,
            width = 100,
            height = 100
        )

    private fun video(): MessagePart =
        Video(
            id = "video-1",
            mimeType = "video/mp4",
            byteSize = 1L,
            width = 1920,
            height = 1080,
            durationMilliseconds = 1_000L
        )

    private fun file(index: Int, id: String = "file-$index"): MessagePart =
        File(
            id = id,
            mimeType = "application/pdf",
            byteSize = 1L,
            fileName = "file-$index.pdf"
        )
}
