package com.cbgm.sparrow.feature.chats.presentation.group.mapper

import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.group.ChatMessageType
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.presentation.component.model.DeliveryProgressModel
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageMediaAttachmentModel
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMemberProgressUi
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupMessageUiModel
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupUiState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.presentation.mapper.toWarningUiModel

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
            conversation.toMessageUiModels(
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
        memberProgress = conversation.toMemberProgress(contactsById)
    )
}

internal fun GroupMessage.toUiModel(
    senderName: String?,
    senderIsInContacts: Boolean,
    senderProfilePictureBytes: ByteArray?,
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentBytes: Map<String, ByteArray> = emptyMap()
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
                    ),
                safetyWarning =
                    if (
                        isMine ||
                        type != ChatMessageType.USER ||
                        contentStatus != MessageContentStatus.READABLE
                    ) {
                        null
                    } else {
                        safetyAssessments[id]?.toWarningUiModel()
                    },
                mediaAttachments =
                    attachments.map { attachment ->
                        MessageMediaAttachmentModel(
                            id = attachment.id,
                            type = attachment.type,
                            mimeType = attachment.mimeType,
                            width = attachment.width,
                            height = attachment.height,
                            durationMilliseconds = attachment.durationMilliseconds,
                            bytes = attachmentBytes[attachment.id]
                        )
                    }
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
    profilePictures: Map<String, ByteArray?>,
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentBytes: Map<String, ByteArray>
): List<GroupMessageUiModel> =
    buildList {
        for (message in this@toMessageUiModels?.messages.orEmpty().asReversed()) {
            val senderContactId = message.senderContactId
            val sender = senderContactId?.let(contactsById::get)
            val senderIsInContacts = sender?.deviceContactLinkStatus == DeviceContactLinkStatus.LINKED
            add(
                message.toUiModel(
                    senderName = sender.displayNameForChat(senderIsInContacts),
                    senderIsInContacts = senderIsInContacts,
                    senderProfilePictureBytes = senderContactId?.let(profilePictures::get),
                    safetyAssessments = safetyAssessments,
                    attachmentBytes = attachmentBytes
                )
            )
        }
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
