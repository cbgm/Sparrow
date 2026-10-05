package com.cbgm.sparrow.core.messagepart.data.mapper

import com.cbgm.sparrow.core.blob.data.model.EncryptedBlobReferenceDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.PollDto
import com.cbgm.sparrow.core.messagepart.data.model.PollOptionDto
import com.cbgm.sparrow.core.messagepart.data.model.VoiceDto
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.Voice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessagePartDtoMapperTest {
    @Test
    fun pollMapsImagesAndOptionsToDomain() {
        val dto =
            PollDto(
                id = "poll-1",
                question = "Question?",
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

        val poll = assertIs<Poll>(dto.toMessagePart())

        assertEquals("Option", poll.options.single().text)
        assertEquals("image-1", poll.images.single().id)
        assertEquals(10, poll.images.single().width)
        assertEquals(20, poll.images.single().height)
    }

    @Test
    fun voiceMapsLocalFilePathToDomainWithoutBlobInfrastructure() {
        val voice =
            assertIs<Voice>(
                VoiceDto(
                    id = "voice-1",
                    blob = blobReference(),
                    mimeType = "audio/wav",
                    byteSize = 512L,
                    durationMilliseconds = 2_000L,
                    localFilePath = "/cache/voice.wav"
                ).toMessagePart()
            )

        assertEquals("/cache/voice.wav", voice.localFilePath)
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
