package com.cbgm.sparrow.feature.chats.domain.model

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
    val textPart = filterIsInstance<MessagePart.Text>().firstOrNull()
    if (textPart?.text.isNullOrBlank()) return false

    return none { part ->
        part is MessagePart.File ||
            part is MessagePart.ImageVideo ||
            part is MessagePart.Location ||
            part is MessagePart.Contact
    }
}
