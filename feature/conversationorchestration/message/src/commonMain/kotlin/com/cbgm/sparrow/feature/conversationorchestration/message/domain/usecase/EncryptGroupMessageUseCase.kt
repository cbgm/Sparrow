package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.GroupMessagePlaintextCodec
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket

/** Encrypts a stored group message for its recipients; every recipient gets its own packet ID. */
class EncryptGroupMessageUseCase(
    private val plaintextCodec: GroupMessagePlaintextCodec,
    private val encryptGroupPayload: EncryptGroupPayloadUseCase
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
                parts = parts,
                replyToMessageId = message.replyToMessageId
            ),
            recipients = recipients,
            packetIdFor = { contactId -> "group-message-${message.id}-$contactId" }
        )
}
