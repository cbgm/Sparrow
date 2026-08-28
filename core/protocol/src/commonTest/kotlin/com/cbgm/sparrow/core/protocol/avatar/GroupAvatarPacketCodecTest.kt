package com.cbgm.sparrow.core.protocol.avatar

import com.cbgm.sparrow.core.protocol.codec.KotlinxPacketCodec
import com.cbgm.sparrow.core.protocol.codec.createProtocolJson
import com.cbgm.sparrow.core.protocol.packet.GroupAvatarUpdatedPacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GroupAvatarPacketCodecTest {
    private val codec = KotlinxPacketCodec(createProtocolJson())

    @Test
    fun groupAvatarUpdateRoundTrips() {
        val original =
            GroupAvatarUpdatedPacket(
                packetId = "avatar-packet-1",
                groupId = "group-1",
                epoch = 3,
                avatar =
                    GroupAvatarMetadata(
                        changedAtEpochMilliseconds = 42L,
                        hasAvatar = true,
                        payload = GroupAvatarPayload(byteArrayOf(1, 2, 3))
                    ),
                adminSigningPublicKey = byteArrayOf(4, 5),
                adminSignature = byteArrayOf(6, 7)
            )

        val decoded =
            assertIs<GroupAvatarUpdatedPacket>(
                codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
            )

        assertEquals(original.packetId, decoded.packetId)
        assertEquals(original.groupId, decoded.groupId)
        assertEquals(original.epoch, decoded.epoch)
        assertEquals(42L, decoded.avatar.changedAtEpochMilliseconds)
        assertEquals(true, decoded.avatar.hasAvatar)
        assertContentEquals(byteArrayOf(1, 2, 3), requireNotNull(decoded.avatar.payload).bytes)
        assertContentEquals(original.adminSigningPublicKey, decoded.adminSigningPublicKey)
        assertContentEquals(original.adminSignature, decoded.adminSignature)
    }
}
