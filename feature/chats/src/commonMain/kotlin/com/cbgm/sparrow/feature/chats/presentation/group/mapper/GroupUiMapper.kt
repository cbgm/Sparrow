package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessagePart
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toMessagePartsUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.DeliveryProgressUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageReactionUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageReplyUi
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMemberProgressUi
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMessageUi
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupUiState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.presentation.details.mapper.toMessageSafetyWarningUi

internal fun toGroupUiState(
    conversation: GroupConversation?,
    administration: GroupAdministrationState,
    contacts: List<Contact>,
    profilePictures: Map<String, ByteArray?>,
    avatarBytes: ByteArray?,
    currentText: String,
    currentError: String?,
    observationError: String?,
    isLoading: Boolean,
    typingContactIds: Set<String>,
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentBytes: Map<String, ByteArray> = emptyMap(),
    currentReplyToMessageId: String? = null
): GroupUiState {
    val contactsById = contacts.associateBy(Contact::id)
    val groupState = conversation?.state ?: GroupConversationState.READY
    val conversationMessages = conversation?.messages.orEmpty()
    val messagesById = conversationMessages.associateBy(GroupMessage::id)

    return GroupUiState(
        title = conversation?.title.orEmpty(),
        avatarBytes = avatarBytes,
        messages =
            conversation.toGroupMessagesUi(
                contactsById = contactsById,
                profilePictures = profilePictures,
                safetyAssessments = safetyAssessments,
                attachmentBytes = attachmentBytes
            ),
        messageText = currentText,
        replyTo = currentReplyToMessageId.toGroupReplyPreview(messagesById, contactsById),
        isSomeoneTyping = typingContactIds.isNotEmpty(),
        typingDisplayName = typingContactIds.toTypingDisplayName(contactsById),
        errorMessage = currentError ?: observationError,
        isLoading = isLoading,
        isMessageInputEnabled = conversation.isMessageInputEnabled(groupState),
        state = groupState,
        memberCount = administration.activeMemberCount + (conversation?.pendingParticipantCount ?: 0),
        readyMemberCount = administration.activeMemberCount,
        pendingMemberCount = conversation?.pendingParticipantCount ?: 0,
        showInvitationActions = groupState == GroupConversationState.INVITED,
        memberProgress = conversation.toGroupMemberProgressUi(contactsById)
    )
}

internal fun GroupMessage.toGroupMessageUi(
    senderName: String?,
    senderIsInContacts: Boolean,
    senderProfilePictureBytes: ByteArray?,
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentBytes: Map<String, ByteArray> = emptyMap(),
    reply: MessageReplyUi? = null
): GroupMessageUi {
    val partsUi = parts.toMessagePartsUi(attachmentBytes)

    return GroupMessageUi(
        bubble =
            MessageBubbleUi(
                id = id,
                isMine = isMine,
                security = security,
                contentStatus = contentStatus,
                deliveryStatus = deliveryStatus,
                senderName = senderName,
                senderIsInContacts = senderIsInContacts,
                deliveryProgress =
                    DeliveryProgressUi(
                        recipientCount = deliveryProgress.recipientCount,
                        deliveredCount = deliveryProgress.deliveredCount,
                        readCount = deliveryProgress.readCount
                    ),
                safetyWarning =
                    if (
                        isMine ||
                        type != ChatMessageType.USER ||
                        contentStatus != MessageContentStatus.READABLE
                    ) {
                        null
                    } else {
                        safetyAssessments[id]?.toMessageSafetyWarningUi()
                    },
                reply = reply,
                reactions = reactions.groupBy { it.emoji }.map { (emoji, values) ->
                    MessageReactionUi(emoji = emoji, count = values.size, reactedByMe = values.any { it.isMine })
                },
                imageVideoParts = partsUi.filterIsInstance<MessagePartUi.ImageVideo>(),
                fileParts = partsUi.filterIsInstance<MessagePartUi.File>(),
                locationPart = partsUi.filterIsInstance<MessagePartUi.Location>().firstOrNull(),
                contactPart = partsUi.filterIsInstance<MessagePartUi.Contact>().firstOrNull(),
                textPart = partsUi.filterIsInstance<MessagePartUi.Text>().firstOrNull()
            ),
        type = type,
        senderContactId = senderContactId,
        senderProfilePictureBytes = senderProfilePictureBytes
    )
}

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

private fun GroupConversation?.toGroupMessagesUi(
    contactsById: Map<String, Contact>,
    profilePictures: Map<String, ByteArray?>,
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentBytes: Map<String, ByteArray>
): List<GroupMessageUi> {
    val messages = this?.messages.orEmpty()
    val messagesById = messages.associateBy(GroupMessage::id)

    return buildList {
        for (message in messages.asReversed()) {
            val senderContactId = message.senderContactId
            val sender = senderContactId?.let(contactsById::get)
            val senderIsInContacts = sender?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
            add(
                message.toGroupMessageUi(
                    senderName = sender.displayNameForChat(senderIsInContacts),
                    senderIsInContacts = senderIsInContacts,
                    senderProfilePictureBytes = senderContactId?.let(profilePictures::get),
                    safetyAssessments = safetyAssessments,
                    attachmentBytes = attachmentBytes,
                    reply = message.replyToMessageId.toGroupReplyPreview(messagesById, contactsById)
                )
            )
        }
    }
}

private fun String?.toGroupReplyPreview(
    messagesById: Map<String, GroupMessage>,
    contactsById: Map<String, Contact>
): MessageReplyUi? =
    this?.let { messageId ->
        val target = messagesById[messageId]
        val sender = target?.senderContactId?.let(contactsById::get)
        val senderIsInContacts = sender?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
        MessageReplyUi(
            messageId = messageId,
            isMine = target?.isMine,
            senderName =
                if (target?.isMine == false) {
                    sender.displayNameForChat(senderIsInContacts)
                } else {
                    null
                },
            previewText = target?.parts.toReplyPreviewText()
        )
    }

private fun List<MessagePart>?.toReplyPreviewText(): String? =
    this
        ?.filterIsInstance<MessagePart.Text>()
        ?.firstOrNull()
        ?.text
        ?.takeIf(String::isNotBlank)
        ?: this
            ?.filterIsInstance<MessagePart.File>()
            ?.firstOrNull()
            ?.fileName
            ?.takeIf(String::isNotBlank)

private fun GroupConversation?.toGroupMemberProgressUi(
    contactsById: Map<String, Contact>
): List<GroupMemberProgressUi> =
    this
        ?.memberInvitationStates
        .orEmpty()
        .takeIf { this?.isIncomingInvitation == false }
        .orEmpty()
        .map { member ->
            val contact = contactsById[member.contactId]
            val isInContacts = contact?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
            GroupMemberProgressUi(
                displayName = contact.displayNameForChat(isInContacts),
                status = member.status
            )
        }

private fun GroupConversation?.isMessageInputEnabled(state: GroupConversationState): Boolean {
    this ?: return false
    return isReady || (!isIncomingInvitation && state.canQueueMessagesWhilePreparing())
}

private fun GroupConversationState.canQueueMessagesWhilePreparing(): Boolean =
    this == GroupConversationState.WAITING_FOR_MEMBERS ||
        this == GroupConversationState.DISTRIBUTING_KEYS

private fun Set<String>.toTypingDisplayName(contactsById: Map<String, Contact>): String =
    mapNotNull(contactsById::get)
        .map { contact ->
            val isInContacts = contact.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
            contact.displayNameForChat(isInContacts)
        }.filter(String::isNotBlank)
        .joinToString(", ")
