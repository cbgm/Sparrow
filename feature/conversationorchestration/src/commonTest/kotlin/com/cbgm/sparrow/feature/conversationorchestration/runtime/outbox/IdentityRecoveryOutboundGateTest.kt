package com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox

import com.cbgm.sparrow.core.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.ContactInvitePacket
import com.cbgm.sparrow.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.sparrow.core.protocol.packet.MessageDeletionPacket
import com.cbgm.sparrow.core.protocol.packet.MessageEditPacket
import com.cbgm.sparrow.core.protocol.packet.ReadReceiptPacket
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Direct packet categories blocked while a replacement identity awaits verification. */
class IdentityRecoveryOutboundGateTest {
    @Test fun directContentAndMetadataRequireStableIdentity() {
        assertTrue(blocksDirectPacketDuringIdentityReview(ChatMessagePacket::class))
        assertTrue(blocksDirectPacketDuringIdentityReview(ReadReceiptPacket::class))
        assertTrue(blocksDirectPacketDuringIdentityReview(MessageEditPacket::class))
        assertTrue(blocksDirectPacketDuringIdentityReview(MessageDeletionPacket::class))
    }

    @Test fun signedInvitationsAndGroupChatRemainOutsideDirectGate() {
        assertFalse(blocksDirectPacketDuringIdentityReview(ContactInvitePacket::class))
        assertFalse(blocksDirectPacketDuringIdentityReview(GroupChatMessagePacket::class))
    }
}
