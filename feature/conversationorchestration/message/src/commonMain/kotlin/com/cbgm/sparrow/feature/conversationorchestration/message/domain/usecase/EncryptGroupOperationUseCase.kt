package com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase

import com.cbgm.sparrow.core.util.id.IdGenerator
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.packet.GroupMessagePlaintextCodec
import com.cbgm.sparrow.protocol.message.MessageOperation
import com.cbgm.sparrow.protocol.message.OperationMessage
import com.cbgm.sparrow.protocol.packet.GroupChatMessagePacket

/** Encrypts an edit, delete, reaction or poll operation as a new group event for the recipients. */
class EncryptGroupOperationUseCase(
    private val plaintextCodec: GroupMessagePlaintextCodec,
    private val encryptGroupPayload: EncryptGroupPayloadUseCase
) {
    suspend operator fun invoke(
        groupId: String,
        operation: MessageOperation,
        recipients: List<String>
    ): Result<Map<String, GroupChatMessagePacket>> {
        val eventId = IdGenerator.generate(prefix = "group-operation")
        return encryptGroupPayload(
            groupId = groupId,
            messageId = eventId,
            sentAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
            plaintext = plaintextCodec.encodeOperation(OperationMessage(operation)),
            recipients = recipients,
            packetIdFor = { contactId -> "group-operation-$eventId-$contactId" }
        )
    }
}
