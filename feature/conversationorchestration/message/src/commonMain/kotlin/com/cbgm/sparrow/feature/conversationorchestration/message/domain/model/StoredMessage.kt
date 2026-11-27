package com.cbgm.sparrow.feature.conversationorchestration.message.domain.model

import com.cbgm.sparrow.core.base.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.base.message.domain.model.MessageTransportMode

/** A persisted conversation message as the write workflows see it. */
data class StoredMessage(
    val id: String,
    val conversationId: String,
    val packetId: String?,
    val replyToMessageId: String?,
    val transportPayload: String?,
    val transportMode: String,
    val contentStatus: MessageContentStatus,
    val deliveryStatus: MessageDeliveryStatus,
    val senderContactId: String?,
    val readReceiptSent: Boolean,
    val isMine: Boolean,
    val createdAtEpochMilliseconds: Long
) {
    /** A message a group member wrote; system messages (member added, left, ...) are local projections. */
    val isGroupUserMessage: Boolean
        get() = transportMode == MessageTransportMode.GROUP_END_TO_END_ENCRYPTED

    /** A direct message still waiting for authorization never reached the contact; changes to it stay local. */
    val wasNeverSent: Boolean
        get() = deliveryStatus == MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION

    /**
     * Whether an incoming packet carrying this message's ID is the same message delivered again rather than a
     * conflicting one. Direct receivers additionally require the stored message to be incoming.
     */
    fun isRedeliveryOf(conversationId: String, packetId: String, senderContactId: String): Boolean =
        this.conversationId == conversationId &&
            this.packetId == packetId &&
            this.senderContactId == senderContactId
}
