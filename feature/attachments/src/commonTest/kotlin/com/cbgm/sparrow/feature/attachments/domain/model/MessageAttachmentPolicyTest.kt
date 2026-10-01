package com.cbgm.sparrow.feature.attachments.domain.model

import kotlin.test.Test
import kotlin.test.assertFailsWith

class MessageAttachmentPolicyTest {
    @Test
    fun `allows up to eight mixed attachments`() {
        MessageAttachmentPolicy.requireValid(
            listOf(image(0), video(1), image(2)) + List(5) { index -> file(index) }
        )
    }

    @Test
    fun `rejects more than eight mixed attachments`() {
        assertFailsWith<IllegalArgumentException> {
            MessageAttachmentPolicy.requireValid(
                List(4) { index -> image(index) } + List(5) { index -> file(index) }
            )
        }
    }

    @Test
    fun `rejects duplicate ids across media and files`() {
        assertFailsWith<IllegalArgumentException> {
            MessageAttachmentPolicy.requireValid(
                listOf(image(1, id = "same-id"), file(1, id = "same-id"))
            )
        }
    }

    @Test
    fun `video requires video mime type`() {
        assertFailsWith<IllegalArgumentException> {
            OutgoingMessageAttachment.Video(
                id = "video-1",
                bytes = byteArrayOf(1),
                mimeType = "image/jpeg"
            )
        }
    }

    private fun image(index: Int, id: String = "image-$index") =
        OutgoingMessageAttachment.Image(
            id = id,
            bytes = byteArrayOf(index.toByte()),
            mimeType = "image/jpeg",
            width = 100,
            height = 100
        )

    private fun video(index: Int) =
        OutgoingMessageAttachment.Video(
            id = "video-$index",
            bytes = byteArrayOf(index.toByte()),
            mimeType = "video/mp4",
            width = 1920,
            height = 1080,
            durationMilliseconds = 1_000L
        )

    private fun file(index: Int, id: String = "file-$index") =
        OutgoingMessageAttachment.File(
            id = id,
            bytes = byteArrayOf(index.toByte()),
            mimeType = "application/pdf",
            fileName = "file-$index.pdf"
        )
}
