package com.cbgm.sparrow.feature.conversationorchestration.message.runtime

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.DirectPendingAuthorizationMessagePolicy
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.model.StoredMessage
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.repository.DirectMessageRepository
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.DiscardDirectMessagesUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.milliseconds

/** App-lifetime cleanup of direct messages that waited too long for authorization. */
class PendingAuthorizationCleanupRunner internal constructor(
    private val messageRepository: DirectMessageRepository,
    private val discardDirectMessages: DiscardDirectMessagesUseCase
) {
    suspend fun run() {
        messageRepository.observeWaitingForAuthorization().collectLatest { messages ->
            if (messages.isEmpty()) return@collectLatest

            discardExpired(messages)

            val remaining = messages.filterNot { message -> message.isExpired(SystemClock.nowEpochMilliseconds()) }
            val nextMessage =
                remaining.minByOrNull { message -> message.createdAtEpochMilliseconds } ?: return@collectLatest
            val delayMilliseconds =
                DirectPendingAuthorizationMessagePolicy
                    .expiresAtEpochMilliseconds(nextMessage.createdAtEpochMilliseconds)
                    .minus(SystemClock.nowEpochMilliseconds())
                    .coerceAtLeast(0L)

            delay(delayMilliseconds.milliseconds)
            discardExpired(remaining)
        }
    }

    private suspend fun discardExpired(messages: List<StoredMessage>) {
        val now = SystemClock.nowEpochMilliseconds()
        discardDirectMessages(messages.filter { message -> message.isExpired(now) }).getOrThrow()
    }

    private fun StoredMessage.isExpired(nowEpochMilliseconds: Long): Boolean =
        DirectPendingAuthorizationMessagePolicy.isExpired(
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            nowEpochMilliseconds = nowEpochMilliseconds
        )
}
