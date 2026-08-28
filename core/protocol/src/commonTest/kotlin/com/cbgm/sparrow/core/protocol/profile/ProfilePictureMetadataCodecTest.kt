package com.cbgm.sparrow.core.protocol.profile

import com.cbgm.sparrow.core.protocol.codec.KotlinxPacketCodec
import com.cbgm.sparrow.core.protocol.codec.createProtocolJson
import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProfilePictureMetadataCodecTest {
    private val codec = KotlinxPacketCodec(createProtocolJson())

    @Test
    fun chatMessageProfilePictureRoundTrips() {
        val original =
            ChatMessagePacket(
                packetId = "packet-1",
                messageId = "message-1",
                sentAtEpochMilliseconds = 100L,
                text = "hello",
                profilePicture =
                    ProfilePictureMetadata(
                        changedAtEpochMilliseconds = 80L,
                        hasPicture = true,
                        payload = ProfilePicturePayload(byteArrayOf(1, 2, 3))
                    )
            )

        val decoded =
            assertIs<ChatMessagePacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(80L, decoded.profilePicture.changedAtEpochMilliseconds)
        assertEquals(true, decoded.profilePicture.hasPicture)
        assertContentEquals(byteArrayOf(1, 2, 3), requireNotNull(decoded.profilePicture.payload).bytes)
    }
}
