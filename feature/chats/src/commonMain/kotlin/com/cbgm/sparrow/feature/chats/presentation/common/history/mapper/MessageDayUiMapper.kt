package com.cbgm.sparrow.feature.chats.presentation.common.history.mapper

import com.cbgm.sparrow.feature.chats.domain.model.MessageDay
import com.cbgm.sparrow.feature.chats.presentation.common.history.model.MessageDayUi

internal fun MessageDay.toMessageDayUi(): MessageDayUi =
    MessageDayUi(
        epochDay = epochDay,
        firstMessageId = firstMessage.messageId,
        firstMessageAt = firstMessage.createdAtEpochMilliseconds,
        messageCount = messageCount
    )
