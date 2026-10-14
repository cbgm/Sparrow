package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.GroupMessagePlaintextCodec
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.ExpenseReceiptRepository
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket

/** Encrypts a stored group message for its recipients; every recipient gets its own packet ID. */
class EncryptGroupMessageUseCase(
    private val plaintextCodec: GroupMessagePlaintextCodec,
    private val encryptGroupPayload: EncryptGroupPayloadUseCase,
    private val expenseReceipts: ExpenseReceiptRepository
) {
    suspend operator fun invoke(
        message: StoredMessage,
        text: String,
        parts: List<MessagePart>,
        recipients: List<String>
    ): Result<Map<String, GroupChatMessagePacket>> =
        encryptGroupPayload(
            groupId = message.conversationId,
            messageId = message.id,
            sentAtEpochMilliseconds = message.createdAtEpochMilliseconds,
            plaintext = plaintextCodec.encodeMessage(
                messageId = message.id,
                text = text,
                parts = parts.map { part -> part.withReceiptBytes(message.conversationId) },
                replyToMessageId = message.replyToMessageId
            ),
            recipients = recipients,
            packetIdFor = { contactId -> "group-message-${message.id}-$contactId" }
        )

    private suspend fun MessagePart.withReceiptBytes(groupId: String): MessagePart {
        if (this !is Expense) return this
        val receipt = receipt ?: return this
        val bytes = expenseReceipts.read(groupId, receipt.id).getOrThrow()
        return copy(receipt = receipt.copy(bytes = bytes))
    }
}
