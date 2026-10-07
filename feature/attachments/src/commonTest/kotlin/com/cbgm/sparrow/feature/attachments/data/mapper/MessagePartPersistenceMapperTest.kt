package com.cbgm.sparrow.feature.attachments.data.mapper

import com.cbgm.sparrow.core.blob.data.model.EncryptedBlobReferenceDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.PollDto
import com.cbgm.sparrow.core.messagepart.data.model.PollOptionDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessagePartPersistenceMapperTest {
    @Test
    fun pollPayloadReloadUsesCurrentNestedBlobReferences() {
        val reference = EncryptedBlobReferenceDto(
            nodeId = "node",
            blobId = "uploaded",
            readCapability = "read",
            ciphertextByteSize = 26,
            expiresAtEpochMilliseconds = 10_000,
            encryptionKey = ByteArray(32),
            nonce = ByteArray(12),
            ciphertextSha256 = ByteArray(32)
        )
        val image = ImageDto(
            id = "image",
            mimeType = "image/jpeg",
            byteSize = 10,
            width = 20,
            height = 30
        )
        val poll = PollDto(
            id = "poll",
            question = "Where?",
            images = listOf(image),
            options = listOf(PollOptionDto("a", "Here"), PollOptionDto("b", "There"))
        )
        val persistedParts = listOf(poll).flattenForPersistence()
        val parts = persistedParts.mapIndexed { index, part ->
            part.toMessagePartEntity("message", index + 1)
        }
        val restored = parts.toDtos(
            blobs = listOf(
                image.copy(blob = reference).toMessageBlobEntity("delete", "cached.jpg")
            ),
            resolveLocalFilePath = { "/cache/$it" }
        )

        val result = assertIs<PollDto>(restored.single())
        assertEquals(reference, result.images.single().blob)
        assertEquals("/cache/cached.jpg", result.images.single().localFilePath)
        assertEquals(poll.options, result.options)
    }

    @Test
    fun pollPayloadWithoutNestedBlobsNeedsNoBlobRows() {
        val poll = PollDto(id = "poll", question = "Where?")
        val restored = listOf(poll.toMessagePartEntity("message", 1)).toDtos(
            blobs = emptyList(),
            resolveLocalFilePath = { it }
        )

        assertEquals(poll, restored.single())
    }
}
