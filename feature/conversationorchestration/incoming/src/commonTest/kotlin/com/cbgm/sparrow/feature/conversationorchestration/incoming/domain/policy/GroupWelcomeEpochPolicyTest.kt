package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.policy

import com.cbgm.sparrow.protocol.packet.GroupCreatedPacket
import com.cbgm.sparrow.protocol.packet.GroupMemberPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupWelcomeEpochPolicyTest {
    @Test
    fun aWelcomeThatSkipsAnEpochWaitsForTheOneBeforeIt() {
        assertEquals(4, GroupPacketEpochPolicy.requiredEpoch(welcome(epoch = 5), currentEpoch = 3))
    }

    @Test
    fun aWelcomeThatRepeatsOrAdvancesByOneIsHandledNow() {
        assertNull(GroupPacketEpochPolicy.requiredEpoch(welcome(epoch = 3), currentEpoch = 3))
        assertNull(GroupPacketEpochPolicy.requiredEpoch(welcome(epoch = 4), currentEpoch = 3))
    }

    @Test
    fun theFirstWelcomeOfAGroupNeverWaits() {
        assertNull(GroupPacketEpochPolicy.requiredEpoch(welcome(epoch = 7), currentEpoch = null))
    }

    private fun welcome(epoch: Int) =
        GroupCreatedPacket(
            packetId = "group-welcome-g-i-$epoch",
            groupId = "g",
            title = "Group",
            createdAtEpochMilliseconds = 1L,
            epoch = epoch,
            members = listOf(GroupMemberPayload(null, byteArrayOf(3), byteArrayOf(4), "OWNER", "+491")),
            wrappedGroupKey = byteArrayOf(1),
            ownerSignature = byteArrayOf(2)
        )
}
