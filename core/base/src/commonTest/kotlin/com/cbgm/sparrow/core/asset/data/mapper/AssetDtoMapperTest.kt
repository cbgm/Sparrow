package com.cbgm.sparrow.core.asset.data.mapper

import com.cbgm.sparrow.core.asset.data.model.EncryptedBlobReferenceDto
import com.cbgm.sparrow.core.asset.data.model.ImageDto
import com.cbgm.sparrow.core.asset.data.model.PollDto
import com.cbgm.sparrow.core.asset.data.model.PollOptionDto
import com.cbgm.sparrow.core.asset.domain.model.Poll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AssetDtoMapperTest {
    @Test
    fun pollMapsImagesAndOptionsToDomain() {
        val dto =
            PollDto(
                id = "poll-1",
                question = "Question",
                options = listOf(PollOptionDto(id = "option-1", text = "Option")),
                images =
                    listOf(
                        ImageDto(
                            id = "image-1",
                            blob = blobReference(),
                            mimeType = "image/jpeg",
                            byteSize = 123L,
                            width = 10,
                            height = 20
                        )
                    )
            )

        val poll = assertIs<Poll>(dto.toAsset())

        assertEquals("Option", poll.options.single().text)
        assertEquals("image-1", poll.images.single().id)
        assertEquals(10, poll.images.single().width)
        assertEquals(20, poll.images.single().height)
    }

    private fun blobReference() =
        EncryptedBlobReferenceDto(
            nodeId = "node",
            blobId = "blob",
            readCapability = "capability",
            ciphertextByteSize = 123L,
            expiresAtEpochMilliseconds = 1L,
            encryptionKey = ByteArray(32),
            nonce = ByteArray(24),
            ciphertextSha256 = ByteArray(32)
        )
}
