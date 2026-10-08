package com.cbgm.sparrow.feature.chats.domain.model

import com.cbgm.sparrow.core.messagepart.domain.model.Contact
import com.cbgm.sparrow.core.messagepart.domain.model.Expense
import com.cbgm.sparrow.core.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.messagepart.domain.model.File
import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.core.messagepart.domain.model.Location
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.core.messagepart.domain.model.Video
import com.cbgm.sparrow.core.messagepart.domain.model.Voice
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessage
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage

fun DirectMessage.isEditable(): Boolean =
    isMine &&
        deliveryStatus != MessageDeliveryStatus.READ &&
        hasEditableTextOnlyContent()

fun GroupMessage.isEditable(): Boolean =
    type == ChatMessageType.USER &&
        isMine &&
        deliveryProgress.readCount == 0 &&
        hasEditableTextOnlyContent()

private fun DirectMessage.hasEditableTextOnlyContent(): Boolean =
    parts.hasEditableTextOnlyContent()

private fun GroupMessage.hasEditableTextOnlyContent(): Boolean =
    parts.hasEditableTextOnlyContent()

private fun List<MessagePart>.hasEditableTextOnlyContent(): Boolean {
    val textPart = filterIsInstance<Text>().firstOrNull()
    if (textPart?.text.isNullOrBlank()) return false

    return none { part ->
        part is File ||
            part is Image ||
            part is Video ||
            part is Location ||
            part is Contact ||
            part is Voice ||
            part is Poll ||
            part is Expense ||
            part is ExpenseBoard
    }
}
