package com.cbgm.sparrow.core.protocol.codec

import com.cbgm.sparrow.core.protocol.attachment.EncryptedBlobReference
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachment
import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.DeliveryReceiptPacket
import com.cbgm.sparrow.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.sparrow.core.protocol.packet.IdentityPacket
import com.cbgm.sparrow.core.protocol.packet.ReadReceiptPacket
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
                text =
                    "Hello"
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
            MessageAttachment(
                attachmentId = "attachment-1",
                type = MessageAttachmentType.IMAGE,
                mimeType = "image/jpeg",
                byteSize = 512L,
                width = 1200,
                height = 800,
                blob =
                    EncryptedBlobReference(
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
                text = "",
                attachments = listOf(attachment)
            )

        val encoded = codec.encode(original).getOrThrow()
        val decoded = codec.decode(encoded).getOrThrow()
        val packet = assertIs<ChatMessagePacket>(decoded)
        val decodedAttachment = packet.attachments.single()

        assertEquals(attachment.attachmentId, decodedAttachment.attachmentId)
        assertEquals(attachment.type, decodedAttachment.type)
        assertEquals(attachment.mimeType, decodedAttachment.mimeType)
        assertEquals(attachment.byteSize, decodedAttachment.byteSize)
        assertEquals(attachment.width, decodedAttachment.width)
        assertEquals(attachment.height, decodedAttachment.height)
        assertEquals(attachment.blob.nodeId, decodedAttachment.blob.nodeId)
        assertEquals(attachment.blob.blobId, decodedAttachment.blob.blobId)
        assertEquals(attachment.blob.ciphertextByteSize, decodedAttachment.blob.ciphertextByteSize)
        assertContentEquals(attachment.blob.encryptionKey, decodedAttachment.blob.encryptionKey)
        assertContentEquals(attachment.blob.nonce, decodedAttachment.blob.nonce)
        assertContentEquals(attachment.blob.ciphertextSha256, decodedAttachment.blob.ciphertextSha256)
    }

    @Test
    fun emptyAttachmentListIsNotAddedToLegacyTextPacket() {
        val encoded =
            codec
                .encode(
                    ChatMessagePacket(
                        packetId = "packet-legacy-shape",
                        messageId = "message-legacy-shape",
                        sentAtEpochMilliseconds = 123_456L,
                        text = "Hello"
                    )
                ).getOrThrow()

        val encodedJson = encoded.decodeToString()
        assertFalse("\"attachments\"" in encodedJson)
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
                text =
                    "Hello"
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
