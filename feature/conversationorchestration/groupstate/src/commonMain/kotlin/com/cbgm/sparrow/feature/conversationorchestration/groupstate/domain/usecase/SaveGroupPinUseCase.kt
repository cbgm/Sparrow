package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.Text
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentOperationsRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinSaveResult
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinSlotPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupPinState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupStateVersionPolicy
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.GroupMessageRepository

/**
 * Pins [message] into the group's pin slot as a snapshot signed later with [senderKey].
 *
 * The message is loaded again because resolving [senderKey] suspends: its sender must still be the one the key
 * was resolved for. An open expense board keeps the slot.
 */
class SaveGroupPinUseCase(
    private val messageRepository: GroupMessageRepository,
    private val pinRepository: GroupPinRepository,
    private val attachmentRepository: MessageAttachmentOperationsRepository,
    private val findPinnableGroupMessage: FindPinnableGroupMessageUseCase
) {
    suspend operator fun invoke(
        groupId: String,
        message: StoredMessage,
        senderKey: ByteArray
    ): Result<GroupPinSaveResult> =
        safeSuspendCall(logFailure = false) {
            val current = findPinnableGroupMessage(groupId, message.id).getOrThrow()
            check(current.isMine == message.isMine && current.senderContactId == message.senderContactId) {
                "Pinned message sender changed while resolving the signing identity"
            }
            val previous = pinRepository.find(groupId).getOrThrow()
            when {
                previous?.messageId == message.id -> GroupPinSaveResult.AlreadyPinned
                GroupPinSlotPolicy.expensesKeepSlot(previous, message.id) -> GroupPinSaveResult.SlotHeldByExpenses
                else -> GroupPinSaveResult.Saved(save(groupId, current, senderKey, previous))
            }
        }

    private suspend fun save(
        groupId: String,
        message: StoredMessage,
        senderKey: ByteArray,
        previous: GroupPinState?
    ): GroupPinState {
        val text = messageRepository.findText(message.id).getOrThrow()?.takeIf(String::isNotBlank)
        val changedAt =
            GroupStateVersionPolicy.nextChangedAt(
                previousChangedAtEpochMilliseconds = previous?.changedAtEpochMilliseconds ?: 0L,
                nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
        val pin =
            GroupPinState(
                groupId = groupId,
                messageId = message.id,
                messageSentAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                messageSenderSigningPublicKey = senderKey.copyOf(),
                senderContactId = message.senderContactId,
                isMine = message.isMine,
                parts = listOfNotNull(text?.let { Text(id = message.id, text = it) }) +
                    attachmentRepository.messageParts(message.id).getOrThrow(),
                replyToMessageId = message.replyToMessageId,
                pinnedAtEpochMilliseconds = changedAt,
                changedAtEpochMilliseconds = changedAt
            )
        pinRepository.save(pin).getOrThrow()
        return pin
    }
}
