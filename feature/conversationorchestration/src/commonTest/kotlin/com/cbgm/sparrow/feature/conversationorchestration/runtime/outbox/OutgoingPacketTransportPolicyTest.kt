package com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutgoingPacketTransportPolicyTest {
    private val policy = OutgoingPacketTransportPolicy()
    private val contact = Contact(
        id = "peer",
        displayName = null,
        phoneNumbers = emptyList(),
        preferredPhoneNumberId = null,
        deviceContactId = null,
        deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED,
        sparrowIdentity = null,
        createdAtEpochMilliseconds = 0L,
        updatedAtEpochMilliseconds = 0L
    )

    @Test
    fun textCannotFallBackToPlaintextWhenKeysAreUnavailable() {
        val requirement = policy.resolve(chatPacket(), contact).getOrThrow()
        assertTrue(requirement.requiresEncryption)
        assertFalse(requirement.forcePlaintext)
    }

    private fun chatPacket() = ChatMessagePacket(
        packetId = "packet-1",
        messageId = "message-1",
        sentAtEpochMilliseconds = 1L,
        text = "hello"
    )
}
