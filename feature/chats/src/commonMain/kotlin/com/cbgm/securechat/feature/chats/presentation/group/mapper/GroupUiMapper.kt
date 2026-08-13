package com.cbgm.securechat.feature.chats.presentation.group.mapper

import com.cbgm.securechat.feature.chats.domain.model.group.GroupMessage
import com.cbgm.securechat.feature.chats.domain.model.group.GroupVerificationMembershipStatus
import com.cbgm.securechat.feature.chats.domain.model.group.GroupVerificationState
import com.cbgm.securechat.feature.chats.presentation.component.model.DeliveryProgressModel
import com.cbgm.securechat.feature.chats.presentation.component.model.MessageBubbleModel
import com.cbgm.securechat.feature.chats.presentation.group.model.GroupMessageUiModel
import com.cbgm.securechat.feature.contacts.domain.model.Contact

internal data class GroupMemberCounts(
    val total: Int,
    val active: Int
)

internal fun GroupVerificationState?.toMemberCounts(
    currentMemberContactIds: Set<String>
): GroupMemberCounts? {
    val state = this ?: return null
    val participantRows = state.pairs.distinctBy { pair -> pair.invitationId }
    val hasAuthoritativeState = state.context.isLocalAdmin || participantRows.isNotEmpty()
    if (!hasAuthoritativeState) return null

    val activeCount =
        participantRows.count { pair ->
            pair.membershipStatus == GroupVerificationMembershipStatus.ACTIVE &&
                (pair.contactId == null || pair.contactId in currentMemberContactIds)
        }

    return GroupMemberCounts(
        total = participantRows.size + 1,
        active = activeCount
    )
}

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
        type = type
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
