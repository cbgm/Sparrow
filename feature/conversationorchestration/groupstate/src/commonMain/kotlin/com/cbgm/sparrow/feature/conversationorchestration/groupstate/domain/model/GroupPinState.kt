package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model

import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart

/** The persisted pin slot of one group; [messageId] is null while nothing is pinned. */
data class GroupPinState(
    val groupId: String,
    val messageId: String?,
    val messageSentAtEpochMilliseconds: Long?,
    val messageSenderSigningPublicKey: ByteArray?,
    val senderContactId: String?,
    val isMine: Boolean?,
    val parts: List<MessagePart>?,
    val replyToMessageId: String?,
    val pinnedAtEpochMilliseconds: Long?,
    val changedAtEpochMilliseconds: Long
) {
    /** The pinned expense board that is still open, if any. */
    val activeExpenseBoard: ExpenseBoard?
        get() = parts.orEmpty().filterIsInstance<ExpenseBoard>().firstOrNull { board -> board.closedAtEpochMilliseconds == null }

    /** An open expense board exclusively occupies the pin slot until it is closed. */
    val hasActiveExpenseBoard: Boolean
        get() = activeExpenseBoard != null

    override fun equals(other: Any?): Boolean =
        other is GroupPinState &&
            groupId == other.groupId &&
            messageId == other.messageId &&
            messageSentAtEpochMilliseconds == other.messageSentAtEpochMilliseconds &&
            messageSenderSigningPublicKey.contentEquals(other.messageSenderSigningPublicKey) &&
            senderContactId == other.senderContactId &&
            isMine == other.isMine &&
            parts == other.parts &&
            replyToMessageId == other.replyToMessageId &&
            pinnedAtEpochMilliseconds == other.pinnedAtEpochMilliseconds &&
            changedAtEpochMilliseconds == other.changedAtEpochMilliseconds

    override fun hashCode(): Int = 31 * groupId.hashCode() + changedAtEpochMilliseconds.hashCode()

    companion object {
        fun unpinned(groupId: String, changedAtEpochMilliseconds: Long): GroupPinState =
            GroupPinState(
                groupId = groupId,
                messageId = null,
                messageSentAtEpochMilliseconds = null,
                messageSenderSigningPublicKey = null,
                senderContactId = null,
                isMine = null,
                parts = null,
                replyToMessageId = null,
                pinnedAtEpochMilliseconds = null,
                changedAtEpochMilliseconds = changedAtEpochMilliseconds
            )
    }
}
