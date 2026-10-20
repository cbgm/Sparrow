package com.cbgm.sparrow.feature.conversationorchestration.delivery.domain.model

sealed interface DeliveryEventTarget {
    /** An outbox packet changed state; it may belong to a group recipient or a direct message. */
    data class Packet(
        val packetId: String
    ) : DeliveryEventTarget {
        init {
            require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        }
    }

    /** A receipt or retry addresses one recipient of a message. */
    data class Recipient(
        val messageId: String,
        val contactId: String
    ) : DeliveryEventTarget {
        init {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        }
    }

    /** A direct message addressed by its own ID, e.g. an explicit retry. */
    data class DirectMessage(
        val messageId: String
    ) : DeliveryEventTarget {
        init {
            require(messageId.isNotBlank()) { "Message ID must not be blank" }
        }
    }
}
