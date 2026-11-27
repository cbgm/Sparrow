package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.GroupMessagePlaintextCodec
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket

/** Encrypts a stored group message for its recipients; every recipient gets its own packet ID. */
class EncryptGroupMessageUseCase(
    private val messageRepository: GroupMessageRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val plaintextCodec: GroupMessagePlaintextCodec,
    private val encryptGroupPayload: EncryptGroupPayloadUseCase
) {
    /** A message stored earlier: its text is loaded again and its attachments are prepared for upload. */
    suspend operator fun invoke(
        message: StoredMessage,
        recipients: List<String>
    ): Result<Map<String, GroupChatMessagePacket>> =
        safeSuspendCall(logFailure = false) {
            invoke(
                message = message,
                text = messageRepository.findText(message.id).getOrThrow().orEmpty(),
                parts = attachmentRepository.prepareOutgoing(message.id).getOrThrow(),
                recipients = recipients
            ).getOrThrow()
        }

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
