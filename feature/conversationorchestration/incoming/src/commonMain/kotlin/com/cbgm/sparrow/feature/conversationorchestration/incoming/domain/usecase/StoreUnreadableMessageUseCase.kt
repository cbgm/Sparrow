package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.message.domain.model.MessageContentStatus
import com.cbgm.sparrow.core.message.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository

/** Keeps a message that could not be read, so the user sees that something arrived. */
class StoreUnreadableMessageUseCase(
    private val conversationRepository: DirectConversationRepository,
    private val messageRepository: DirectMessageRepository
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        contactId: String,
        encodedTransportPayload: String,
        text: String,
        transportMode: String,
        contentStatus: MessageContentStatus,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        safeSuspendCall(logFailure = false) {
            val conversationId = conversationRepository.getOrCreate(contactId).getOrThrow()
            messageRepository.saveIncoming(
                message =
                    StoredMessage(
                        id = IdGenerator.generate(prefix = "failed-message"),
                        conversationId = conversationId,
                        packetId = null,
                        replyToMessageId = null,
                        transportPayload = encodedTransportPayload,
                        transportMode = transportMode,
                        contentStatus = contentStatus,
                        deliveryStatus = MessageDeliveryStatus.NOT_APPLICABLE,
                        senderContactId = contactId,
                        readReceiptSent = false,
                        isMine = false,
                        createdAtEpochMilliseconds = receivedAtEpochMilliseconds
                    ),
                text = text,
                receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
            ).getOrThrow()
        }
}
