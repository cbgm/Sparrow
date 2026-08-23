package com.cbgm.sparrow.feature.chats.domain.model.attachment

import kotlin.test.Test
import kotlin.test.assertFailsWith

class MessageAttachmentPolicyTest {
    @Test
    fun `allows up to eight mixed gallery attachments`() {
        val media =
            List(MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE) { index ->
                if (index % 2 == 0) image(index) else video(index)
            }

        MessageAttachmentPolicy.requireValid(media)
    }

    @Test
    fun `rejects more than eight gallery attachments`() {
        assertFailsWith<IllegalArgumentException> {
            MessageAttachmentPolicy.requireValid(
                List(MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE + 1) { index -> image(index) }
            )
        }
    }

    @Test
    fun `rejects duplicate attachment ids`() {
        assertFailsWith<IllegalArgumentException> {
            MessageAttachmentPolicy.requireValid(listOf(image(1), image(1)))
        }
    }

    @Test
    fun `video requires a video mime type`() {
        assertFailsWith<IllegalArgumentException> {
            OutgoingMediaAttachment(
                id = "video-1",
                type = MessageMediaType.VIDEO,
                bytes = byteArrayOf(1),
                mimeType = "image/jpeg"
            )
        }
    }

    private fun image(index: Int) =
        OutgoingMediaAttachment(
            id = "image-$index",
            type = MessageMediaType.IMAGE,
            bytes = byteArrayOf(index.toByte()),
            mimeType = "image/jpeg",
            width = 100,
            height = 100
        )

    private fun video(index: Int) =
        OutgoingMediaAttachment(
            id = "video-$index",
            type = MessageMediaType.VIDEO,
            bytes = byteArrayOf(index.toByte()),
            mimeType = "video/mp4",
            width = 1920,
            height = 1080,
            durationMilliseconds = 1_000L
        )
}
