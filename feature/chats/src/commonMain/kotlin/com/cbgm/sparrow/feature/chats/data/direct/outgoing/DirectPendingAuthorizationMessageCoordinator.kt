package com.cbgm.sparrow.feature.chats.data.direct.outgoing

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.data.database.dao.ChatDao
import com.cbgm.sparrow.data.database.entity.MessageEntity
import com.cbgm.sparrow.feature.chats.data.attachment.MessageAttachmentTransfer
import com.cbgm.sparrow.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectPendingAuthorizationMessagePolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.milliseconds

/**
 * Removes locally queued direct messages that never received fresh authorization.
 *
 * The queue is reactive: a changed message set reschedules the exact next deadline.
 * There is no periodic polling interval.
 */
class DirectPendingAuthorizationMessageCoordinator(
    private val chatDao: ChatDao,
    private val attachmentTransfer: MessageAttachmentTransfer
) {
    suspend fun run() {
        chatDao
            .observeDirectMessagesByDeliveryStatus(MessageDeliveryStatus.WAITING_FOR_AUTHORIZATION.name)
            .collectLatest { messages ->
                if (messages.isEmpty()) return@collectLatest

                deleteExpired(messages)

                val remaining =
                    messages.filterNot { message ->
                        DirectPendingAuthorizationMessagePolicy.isExpired(
                            createdAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                            nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                        )
                    }
                val nextMessage = remaining.minByOrNull { message -> message.createdAtEpochMilliseconds }
                    ?: return@collectLatest
                val delayMilliseconds =
                    DirectPendingAuthorizationMessagePolicy
                        .expiresAtEpochMilliseconds(nextMessage.createdAtEpochMilliseconds)
                        .minus(SystemClock.nowEpochMilliseconds())
                        .coerceAtLeast(0L)

                delay(delayMilliseconds.milliseconds)
                deleteExpired(remaining)
            }
    }

    private suspend fun deleteExpired(messages: List<MessageEntity>) {
        val nowEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        val expired =
            messages.filter { message ->
                DirectPendingAuthorizationMessagePolicy.isExpired(
                    createdAtEpochMilliseconds = message.createdAtEpochMilliseconds,
                    nowEpochMilliseconds = nowEpochMilliseconds
                )
            }
        if (expired.isEmpty()) return
        attachmentTransfer.deleteForMessages(expired.map(MessageEntity::id))
        chatDao.deleteMessagesAndRefreshConversations(expired)
    }
}
