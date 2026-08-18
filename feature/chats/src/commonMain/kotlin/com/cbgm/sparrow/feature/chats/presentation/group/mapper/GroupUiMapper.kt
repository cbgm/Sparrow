package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.presentation.component.model.DeliveryProgressModel
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMessageUiModel
import com.cbgm.sparrow.feature.contacts.domain.model.Contact

internal fun GroupMessage.toUiModel(
    senderName: String?,
    senderIsInContacts: Boolean
): GroupMessageUiModel =
    GroupMessageUiModel(
        bubble =
            MessageBubbleModel(
                id = id,
                text = text,
                isMine = isMine,
                security = security,
                contentStatus = contentStatus,
                deliveryStatus = deliveryStatus,
                senderName = senderName,
                senderIsInContacts = senderIsInContacts,
                deliveryProgress =
                    DeliveryProgressModel(
                        recipientCount = deliveryProgress.recipientCount,
                        deliveredCount = deliveryProgress.deliveredCount,
                        readCount = deliveryProgress.readCount
                    )
            ),
        type = type,
        senderContactId = senderContactId
    )

internal fun Contact?.displayNameForChat(isInContacts: Boolean): String {
    if (this == null) return "Unknown contact"

    return if (isInContacts) {
        displayName?.takeIf(String::isNotBlank)
            ?: preferredPhoneNumber?.value
            ?: "Unknown contact"
    } else {
        preferredPhoneNumber?.value
            ?: displayName?.takeIf(String::isNotBlank)
            ?: "Unknown contact"
    }
}
