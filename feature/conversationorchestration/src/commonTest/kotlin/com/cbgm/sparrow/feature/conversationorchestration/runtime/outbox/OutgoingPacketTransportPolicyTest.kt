package com.cbgm.sparrow.feature.conversationorchestration.runtime.outbox

import com.cbgm.sparrow.core.messagepart.data.model.TextDto
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.message.OperationMessage
import com.cbgm.sparrow.protocol.packet.ChatMessagePacket
import com.cbgm.sparrow.protocol.packet.OperationMessagePacket
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

    @Test
    fun operationCannotFallBackToPlaintextWhenKeysAreUnavailable() {
        val requirement =
            policy.resolve(
                OperationMessagePacket(
                    packetId = "operation-packet-1",
                    message =
                        OperationMessage(
                            MessageOperation.Reaction(
                                messageId = "message-1",
                                emoji = "👍"
                            )
                        )
                ),
                contact
            ).getOrThrow()

        assertTrue(requirement.requiresEncryption)
        assertFalse(requirement.forcePlaintext)
    }

    private fun chatPacket() = ChatMessagePacket(
        packetId = "packet-1",
        messageId = "message-1",
        sentAtEpochMilliseconds = 1L,
        parts = listOf(TextDto(id = "message-1", text = "hello"))
    )
}
