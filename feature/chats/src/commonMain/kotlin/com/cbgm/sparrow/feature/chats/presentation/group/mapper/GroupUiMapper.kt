package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toMessagePartsUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.DeliveryProgressUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
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
    attachmentBytes: Map<String, ByteArray> = emptyMap()
): GroupUiState {
    val contactsById = contacts.associateBy(Contact::id)
    val groupState = conversation?.state ?: GroupConversationState.READY

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
    attachmentBytes: Map<String, ByteArray> = emptyMap()
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
                imageVideoParts = partsUi.filterIsInstance<MessagePartUi.ImageVideoUi>(),
                fileParts = partsUi.filterIsInstance<MessagePartUi.FileUi>(),
                locationPart = partsUi.filterIsInstance<MessagePartUi.LocationUi>().firstOrNull(),
                contactPart = partsUi.filterIsInstance<MessagePartUi.ContactUi>().firstOrNull(),
                textPart = partsUi.filterIsInstance<MessagePartUi.TextUi>().firstOrNull()
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
): List<GroupMessageUi> =
    buildList {
        for (message in this@toGroupMessagesUi?.messages.orEmpty().asReversed()) {
            val senderContactId = message.senderContactId
            val sender = senderContactId?.let(contactsById::get)
            val senderIsInContacts = sender?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
            add(
                message.toGroupMessageUi(
                    senderName = sender.displayNameForChat(senderIsInContacts),
                    senderIsInContacts = senderIsInContacts,
                    senderProfilePictureBytes = senderContactId?.let(profilePictures::get),
                    safetyAssessments = safetyAssessments,
                    attachmentBytes = attachmentBytes
                )
            )
        }
    }

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
