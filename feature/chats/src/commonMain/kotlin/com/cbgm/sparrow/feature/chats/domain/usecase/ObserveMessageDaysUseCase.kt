package com.cbgm.sparrow.feature.chats.domain.usecase

import com.cbgm.sparrow.core.util.time.localEpochDay
import com.cbgm.sparrow.feature.chats.domain.model.MessageDay
import com.cbgm.sparrow.feature.chats.domain.repository.MessageHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The days of a conversation that have messages, newest first. */
class ObserveMessageDaysUseCase(
    private val messageHistoryRepository: MessageHistoryRepository
) {
    operator fun invoke(conversationId: String): Flow<List<MessageDay>> =
        messageHistoryRepository.observeCursors(conversationId).map { cursors ->
            cursors
                .groupBy { cursor -> localEpochDay(cursor.createdAtEpochMilliseconds) }
                .map { (epochDay, dayCursors) ->
                    MessageDay(epochDay = epochDay, firstMessage = dayCursors.last(), messageCount = dayCursors.size)
                }
        }
}
