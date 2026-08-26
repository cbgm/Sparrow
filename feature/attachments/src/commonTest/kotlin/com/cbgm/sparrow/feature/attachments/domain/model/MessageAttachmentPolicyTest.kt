package com.cbgm.sparrow.feature.attachments.domain.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
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
            OutgoingMessageAttachment(
                id = "video-1",
                type = MessageAttachmentType.VIDEO,
                bytes = byteArrayOf(1),
                mimeType = "image/jpeg"
            )
        }
    }

    private fun image(index: Int, id: String = "image-$index") =
        OutgoingMessageAttachment(
            id = id,
            type = MessageAttachmentType.IMAGE,
            bytes = byteArrayOf(index.toByte()),
            mimeType = "image/jpeg",
            width = 100,
            height = 100
        )

    private fun video(index: Int) =
        OutgoingMessageAttachment(
            id = "video-$index",
            type = MessageAttachmentType.VIDEO,
            bytes = byteArrayOf(index.toByte()),
            mimeType = "video/mp4",
            width = 1920,
            height = 1080,
            durationMilliseconds = 1_000L
        )

    private fun file(index: Int, id: String = "file-$index") =
        OutgoingMessageAttachment(
            id = id,
            type = MessageAttachmentType.FILE,
            bytes = byteArrayOf(index.toByte()),
            mimeType = "application/pdf",
            fileName = "file-$index.pdf"
        )
}
