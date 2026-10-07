package com.cbgm.sparrow.protocol.codec

import com.cbgm.sparrow.core.blob.data.model.EncryptedBlobReferenceDto
import com.cbgm.sparrow.core.messagepart.data.model.ImageDto
import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.protocol.packet.IdentityPacket
import com.cbgm.sparrow.protocol.packet.ReadReceiptPacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KotlinxPacketCodecTest {
    private val codec =
        KotlinxPacketCodec(
            json =
                createProtocolJson()
        )

    @Test
    fun chatMessageRoundTrip() {
        val original =
            ChatMessagePacket(
                packetId =
                    "packet-1",
                messageId =
                    "message-1",
                sentAtEpochMilliseconds =
                123_456L,
                parts =
                    listOf(
                        TextDto(
                            id = "message-1",
                            text = "Hello"
                        )
                    )
            )

        val encoded =
            codec
                .encode(
                    packet = original
                ).getOrThrow()

        val decoded =
            codec
                .decode(
                    encodedPacket = encoded
                ).getOrThrow()

        val packet =
            assertIs<ChatMessagePacket>(
                decoded
            )

        assertEquals(
            expected =
            original,
            actual =
            packet
        )
    }

    @Test
    fun chatMessageAttachmentRoundTrip() {
        val attachment =
            ImageDto(
                id = "attachment-1",
                mimeType = "image/jpeg",
                byteSize = 512L,
                width = 1200,
                height = 800,
                localFilePath = "/tmp/local-image",
                thumbnailFilePath = "/tmp/local-thumb",
                bytes = byteArrayOf(9, 8, 7),
                blob =
                    EncryptedBlobReferenceDto(
                        nodeId = "node-a",
                        blobId = "blob-1234567890123456",
                        readCapability = "read-capability",
                        ciphertextByteSize = 528L,
                        expiresAtEpochMilliseconds = 123_456_789L,
                        encryptionKey = ByteArray(32) { 1 },
                        nonce = ByteArray(24) { 2 },
                        ciphertextSha256 = ByteArray(32) { 3 }
                    )
            )
        val original =
            ChatMessagePacket(
                packetId = "packet-attachment-1",
                messageId = "message-attachment-1",
                sentAtEpochMilliseconds = 123_456L,
                parts = listOf(attachment)
            )

        val encoded = codec.encode(original).getOrThrow()
        val encodedJson = encoded.decodeToString()
        assertFalse("localFilePath" in encodedJson)
        assertFalse("thumbnailFilePath" in encodedJson)
        assertFalse("\"bytes\"" in encodedJson)
        val decoded = codec.decode(encoded).getOrThrow()
        val packet = assertIs<ChatMessagePacket>(decoded)
        val decodedAttachment = assertIs<ImageDto>(packet.parts.single())

        assertEquals(attachment.id, decodedAttachment.id)
        assertEquals(attachment.mimeType, decodedAttachment.mimeType)
        assertEquals(attachment.byteSize, decodedAttachment.byteSize)
        assertEquals(attachment.width, decodedAttachment.width)
        assertEquals(attachment.height, decodedAttachment.height)
        val expectedBlob = requireNotNull(attachment.blob)
        val actualBlob = requireNotNull(decodedAttachment.blob)
        assertEquals(expectedBlob.nodeId, actualBlob.nodeId)
        assertEquals(expectedBlob.blobId, actualBlob.blobId)
        assertEquals(expectedBlob.ciphertextByteSize, actualBlob.ciphertextByteSize)
        assertContentEquals(expectedBlob.encryptionKey, actualBlob.encryptionKey)
        assertContentEquals(expectedBlob.nonce, actualBlob.nonce)
        assertContentEquals(expectedBlob.ciphertextSha256, actualBlob.ciphertextSha256)
    }

    @Test
    fun chatMessageUsesMessagePartsField() {
        val encoded =
            codec
                .encode(
                    ChatMessagePacket(
                        packetId = "packet-parts-shape",
                        messageId = "message-parts-shape",
                        sentAtEpochMilliseconds = 123_456L,
                        parts =
                            listOf(
                                TextDto(
                                    id = "message-parts-shape",
                                    text = "Hello"
                                )
                            )
                    )
                ).getOrThrow()

        val encodedJson = encoded.decodeToString()

        assertTrue("\"parts\"" in encodedJson)
        assertFalse("\"attachments\"" in encodedJson)
        assertTrue("\"partType\":\"TEXT\"" in encodedJson)
    }

    @Test
    fun chatMessageReplyRoundTrip() {
        val original =
            ChatMessagePacket(
                packetId = "packet-reply-1",
                messageId = "message-reply-1",
                sentAtEpochMilliseconds = 123_456L,
                parts = listOf(TextDto(id = "message-reply-1", text = "Reply")),
                replyToMessageId = "message-original-1"
            )

        val encoded = codec.encode(original).getOrThrow()
        val decoded = codec.decode(encoded).getOrThrow()
        val packet = assertIs<ChatMessagePacket>(decoded)

        assertEquals("message-original-1", packet.replyToMessageId)
        assertTrue("\"replyToMessageId\":\"message-original-1\"" in encoded.decodeToString())
    }

    @Test
    fun replyFieldIsOmittedWhenAbsent() {
        val encoded =
            codec
                .encode(
                    ChatMessagePacket(
                        packetId = "packet-no-reply",
                        messageId = "message-no-reply",
                        sentAtEpochMilliseconds = 123_456L,
                        parts = listOf(TextDto(id = "message-no-reply", text = "Hello"))
                    )
                ).getOrThrow()

        assertFalse("\"replyToMessageId\"" in encoded.decodeToString())
    }

    @Test
    fun identityRoundTrip() {
        val original =
            IdentityPacket(
                packetId =
                    "packet-identity-1",
                displayName =
                    "Chris",
                encryptionPublicKey =
                    byteArrayOf(
                        1,
                        2,
                        3
                    ),
                signingPublicKey =
                    byteArrayOf(
                        4,
                        5,
                        6
                    )
            )

        val decoded =
            codec
                .decode(
                    encodedPacket =
                        codec
                            .encode(
                                packet = original
                            ).getOrThrow()
                ).getOrThrow()

        val packet =
            assertIs<IdentityPacket>(
                decoded
            )

        assertEquals(
            expected =
                original.packetId,
            actual =
                packet.packetId
        )

        assertEquals(
            expected =
                original.displayName,
            actual =
                packet.displayName
        )

        assertContentEquals(
            expected =
                original.encryptionPublicKey,
            actual =
                packet.encryptionPublicKey
        )

        assertContentEquals(
            expected =
                original.signingPublicKey,
            actual =
                packet.signingPublicKey
        )
    }

    @Test
    fun deliveryReceiptRoundTrip() {
        val original =
            DeliveryReceiptPacket(
                packetId =
                    "delivery-receipt-message-1",
                messageId =
                    "message-1",
                deliveredAtEpochMilliseconds =
                123_456L
            )

        val encoded =
            codec
                .encode(
                    packet = original
                ).getOrThrow()

        val decoded =
            codec
                .decode(
                    encodedPacket = encoded
                ).getOrThrow()

        val receipt =
            assertIs<DeliveryReceiptPacket>(
                decoded
            )

        assertEquals(
            expected = original,
            actual = receipt
        )
    }

    @Test
    fun identityAcknowledgementPacketRoundTrip() {
        val original =
            IdentityAcknowledgementPacket(
                packetId =
                    "identity-acknowledgement-packet-1",
                senderSigningPublicKey =
                    byteArrayOf(
                        1,
                        2,
                        3,
                        4
                    ),
                acknowledgedEncryptionPublicKey =
                    byteArrayOf(
                        5,
                        6,
                        7,
                        8
                    ),
                acknowledgedSigningPublicKey =
                    byteArrayOf(
                        9,
                        10,
                        11,
                        12
                    ),
                signature =
                    byteArrayOf(
                        13,
                        14,
                        15,
                        16
                    )
            )

        val encoded =
            codec
                .encode(
                    packet = original
                ).getOrThrow()

        val decoded =
            codec
                .decode(
                    encodedPacket = encoded
                ).getOrThrow()

        val acknowledgement =
            assertIs<
                IdentityAcknowledgementPacket
            >(
                decoded
            )

        assertEquals(
            expected =
                original.packetId,
            actual =
                acknowledgement.packetId
        )

        assertEquals(
            expected =
                original.version,
            actual =
                acknowledgement.version
        )

        assertContentEquals(
            expected =
                original.senderSigningPublicKey,
            actual =
                acknowledgement
                    .senderSigningPublicKey
        )

        assertContentEquals(
            expected =
                original
                    .acknowledgedEncryptionPublicKey,
            actual =
                acknowledgement
                    .acknowledgedEncryptionPublicKey
        )

        assertContentEquals(
            expected =
                original
                    .acknowledgedSigningPublicKey,
            actual =
                acknowledgement
                    .acknowledgedSigningPublicKey
        )

        assertContentEquals(
            expected =
                original.signature,
            actual =
                acknowledgement.signature
        )
    }

    @Test
    fun readReceiptRoundTrip() {
        val original =
            ReadReceiptPacket(
                packetId =
                    "read-receipt-message-1",
                messageId =
                    "message-1",
                readAtEpochMilliseconds =
                123_456L
            )

        val encoded =
            codec
                .encode(
                    packet = original
                ).getOrThrow()

        val decoded =
            codec
                .decode(
                    encodedPacket = encoded
                ).getOrThrow()

        val receipt =
            assertIs<ReadReceiptPacket>(
                decoded
            )

        assertEquals(
            expected = original,
            actual = receipt
        )
    }

    @Test
    fun packetContainsDiscriminator() {
        val packet =
            ChatMessagePacket(
                packetId =
                    "packet-1",
                messageId =
                    "message-1",
                sentAtEpochMilliseconds =
                1L,
                parts =
                    listOf(
                        TextDto(
                            id = "message-1",
                            text = "Hello"
                        )
                    )
            )

        val encoded =
            codec
                .encode(
                    packet = packet
                ).getOrThrow()
                .decodeToString()

        assertTrue {
            encoded.contains(
                "\"packetType\":\"chat_message\""
            )
        }
    }

    @Test
    fun invalidPacketReturnsFailure() {
        val result =
            codec.decode(
                encodedPacket =
                    "not-json"
                        .encodeToByteArray()
            )

        assertTrue(
            result.isFailure
        )
    }
}
