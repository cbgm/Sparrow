package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessagePart
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.domain.model.group.resolveComposerState
import com.cbgm.sparrow.feature.chats.domain.model.isEditable
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toMessagePartsUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.DeliveryProgressUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageReactionUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageReplyUi
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMemberProgressUi
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMembershipUiState
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMessageUi
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.presentation.details.mapper.toMessageSafetyWarningUi
import kotlin.collections.component1
import kotlin.collections.component2

@Suppress("UNUSED_PARAMETER")
internal fun toGroupConversationUiState(
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
): GroupConversationUiState =
    toGroupConversationUiState(
        conversation = conversation,
        contacts = contacts,
        profilePictures = profilePictures,
        avatarBytes = avatarBytes,
        isLoading = isLoading,
        safetyAssessments = safetyAssessments,
        attachmentBytes = attachmentBytes
    )

internal fun toGroupConversationUiState(
    conversation: GroupConversation?,
    contacts: List<Contact>,
    profilePictures: Map<String, ByteArray?>,
    avatarBytes: ByteArray?,
    isLoading: Boolean,
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentBytes: Map<String, ByteArray> = emptyMap()
): GroupConversationUiState {
    val contactsById = contacts.associateBy(Contact::id)

    return GroupConversationUiState(
        title = conversation?.title.orEmpty(),
        avatarBytes = avatarBytes,
        messages = conversation.toMessageBubbleUi(
            contactsById = contactsById,
            profilePictures = profilePictures,
            safetyAssessments = safetyAssessments,
            attachmentBytes = attachmentBytes
        ),
        isLoading = isLoading,
        state = conversation?.state ?: GroupConversationState.READY,
        composerState = conversation.resolveComposerState()
    )
}

internal fun toGroupMembershipUiState(
    conversation: GroupConversation?,
    administration: GroupAdministrationState,
    contacts: List<Contact>
): GroupMembershipUiState {
    val contactsById = contacts.associateBy(Contact::id)
    val pendingMemberCount = conversation?.pendingParticipantCount ?: 0

    return GroupMembershipUiState(
        memberCount = administration.activeMemberCount + pendingMemberCount,
        readyMemberCount = administration.activeMemberCount,
        pendingMemberCount = pendingMemberCount,
        memberProgress = conversation.toGroupMemberProgressUi(contactsById)
    )
}

internal fun GroupMessage.toMessageBubbleUi(
    senderName: String?,
    senderIsInContacts: Boolean,
    senderProfilePictureBytes: ByteArray?,
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentBytes: Map<String, ByteArray> = emptyMap(),
    reply: MessageReplyUi? = null
): MessageBubbleUi {
    val partsUi = parts.toMessagePartsUi(attachmentBytes)

    return MessageBubbleUi(
        id = id,
        isMine = isMine,
        security = security,
        contentStatus = contentStatus,
        deliveryStatus = deliveryStatus,
        canEdit = isEditable(),
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
            MessageReactionUi(
                emoji = emoji,
                count = values.size,
                reactedByMe = values.any { it.isMine }
            )
        },
        imageVideoParts = partsUi.filterIsInstance<MessagePartUi.ImageVideo>(),
        fileParts = partsUi.filterIsInstance<MessagePartUi.File>(),
        locationPart = partsUi.filterIsInstance<MessagePartUi.Location>().firstOrNull(),
        contactPart = partsUi.filterIsInstance<MessagePartUi.Contact>().firstOrNull(),
        textPart = partsUi.filterIsInstance<MessagePartUi.Text>().firstOrNull(),
        groupExtension = GroupMessageUi(
            type = type,
            senderContactId = senderContactId,
            senderProfilePictureBytes = senderProfilePictureBytes
        )
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

internal fun String?.toGroupReplyPreview(
    conversation: GroupConversation?,
    contacts: List<Contact>
): MessageReplyUi? {
    val contactsById = contacts.associateBy(Contact::id)
    val messagesById = conversation?.messages.orEmpty().associateBy(GroupMessage::id)
    return toGroupReplyPreview(messagesById, contactsById)
}

internal fun Set<String>.toTypingDisplayName(contacts: List<Contact>): String =
    toTypingDisplayName(contacts.associateBy(Contact::id))

private fun GroupConversation?.toMessageBubbleUi(
    contactsById: Map<String, Contact>,
    profilePictures: Map<String, ByteArray?>,
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentBytes: Map<String, ByteArray>
): List<MessageBubbleUi> {
    val messages = this?.messages.orEmpty()
    val messagesById = messages.associateBy(GroupMessage::id)

    return buildList {
        for (message in messages.asReversed()) {
            val senderContactId = message.senderContactId
            val sender = senderContactId?.let(contactsById::get)
            val senderIsInContacts =
                sender?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
            add(
                message.toMessageBubbleUi(
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

private fun Set<String>.toTypingDisplayName(contactsById: Map<String, Contact>): String =
    mapNotNull(contactsById::get)
        .map { contact ->
            val isInContacts = contact.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
            contact.displayNameForChat(isInContacts)
        }.filter(String::isNotBlank)
        .joinToString(", ")
