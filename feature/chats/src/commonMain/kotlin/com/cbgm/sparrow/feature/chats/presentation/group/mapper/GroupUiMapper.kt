package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.presentation.component.model.DeliveryProgressModel
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMemberProgressUi
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMessageUiModel
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupUiState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus

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
    typingContactIds: Set<String>
): GroupUiState {
    val contactsById = contacts.associateBy(Contact::id)
    val groupState = conversation?.state ?: GroupConversationState.READY

    return GroupUiState(
        title = conversation?.title.orEmpty(),
        avatarBytes = avatarBytes,
        messages = conversation.toMessageUiModels(contactsById, profilePictures),
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
        memberProgress = conversation.toMemberProgress(contactsById)
    )
}

internal fun GroupMessage.toUiModel(
    senderName: String?,
    senderIsInContacts: Boolean,
    senderProfilePictureBytes: ByteArray?
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
        senderContactId = senderContactId,
        senderProfilePictureBytes = senderProfilePictureBytes
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

private fun GroupConversation?.toMessageUiModels(
    contactsById: Map<String, Contact>,
    profilePictures: Map<String, ByteArray?>
): List<GroupMessageUiModel> =
    this
        ?.messages
        .orEmpty()
        .asReversed()
        .map { message ->
            val senderContactId = message.senderContactId
            val sender = senderContactId?.let(contactsById::get)
            val senderIsInContacts = sender?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
            message.toUiModel(
                senderName = sender.displayNameForChat(senderIsInContacts),
                senderIsInContacts = senderIsInContacts,
                senderProfilePictureBytes = senderContactId?.let(profilePictures::get)
            )
        }

private fun GroupConversation?.toMemberProgress(
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
