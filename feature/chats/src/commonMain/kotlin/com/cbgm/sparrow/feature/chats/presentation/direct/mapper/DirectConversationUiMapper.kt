package com.cbgm.sparrow.feature.chats.presentation.direct.mapper

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.MessagePart
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessage
import com.cbgm.sparrow.feature.chats.domain.model.direct.resolveDirectComposerState
import com.cbgm.sparrow.feature.chats.domain.model.isEditable
import com.cbgm.sparrow.feature.chats.presentation.component.mapper.toMessagePartsUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageReactionUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageReplyUi
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.presentation.details.mapper.toMessageSafetyWarningUi

internal fun resolveContactName(
    contact: Contact?,
    fallbackContactName: String
): String =
    contact
        ?.displayName
        ?.takeIf(String::isNotBlank)
        ?: fallbackContactName.takeIf(String::isNotBlank)
        ?: "Unknown contact"

internal fun DirectMessage.toMessageBubbleUi(
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentPayloadBytes: Map<String, ByteArray> = emptyMap(),
    reply: MessageReplyUi? = null
): MessageBubbleUi {
    val partsUi = parts.toMessagePartsUi(attachmentPayloadBytes)

    return MessageBubbleUi(
        id = id,
        isMine = isMine,
        security = security,
        contentStatus = contentStatus,
        deliveryStatus = deliveryStatus,
        canEdit = isEditable(),
        safetyWarning =
            if (isMine || contentStatus != MessageContentStatus.READABLE) {
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
    )
}

internal fun String?.toDirectReplyPreview(
    conversation: DirectConversation?,
    contactName: String
): MessageReplyUi? =
    toDirectReplyPreview(conversation?.messages.orEmpty().associateBy(DirectMessage::id), contactName)

private fun String?.toDirectReplyPreview(
    messagesById: Map<String, DirectMessage>,
    contactName: String
): MessageReplyUi? =
    this?.let { messageId ->
        val target = messagesById[messageId]
        MessageReplyUi(
            messageId = messageId,
            isMine = target?.isMine,
            senderName = target?.takeUnless(DirectMessage::isMine)?.let { contactName },
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

internal fun Contact?.toContactSecurityState(): ContactSecurityState {
    val identity = this?.sparrowIdentity ?: return ContactSecurityState.NO_REMOTE_PUBLIC_KEYS

    if (identity.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
        return ContactSecurityState.ONE_WAY_KEYS
    }

    val verifiedByMe = identity.verificationStatus == ContactVerificationStatus.VERIFIED
    val verifiedByContact = identity.verifiedByContact

    return when {
        verifiedByMe && verifiedByContact -> ContactSecurityState.MUTUAL_KEYS_VERIFIED
        verifiedByMe -> ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_ME
        verifiedByContact -> ContactSecurityState.MUTUAL_KEYS_VERIFIED_BY_CONTACT
        else -> ContactSecurityState.MUTUAL_KEYS_UNVERIFIED
    }
}

internal fun isDirectChatAuthorized(
    contact: Contact?,
    identityHandshakeState: IdentityHandshakeState?,
    identitySetupMode: DirectIdentitySetupMode
): Boolean =
    when (identitySetupMode) {
        DirectIdentitySetupMode.AUTOMATIC_INVITATION ->
            identityHandshakeState == IdentityHandshakeState.WAITING_FOR_READY ||
                identityHandshakeState == IdentityHandshakeState.MUTUAL_UNVERIFIED

        DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING ->
            contact?.sparrowIdentity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL
    }

internal fun toDirectConversationUiState(
    contactId: String,
    fallbackContactName: String,
    conversation: DirectConversation?,
    contact: Contact?,
    handshake: IdentityHandshakeState?,
    setupMode: DirectIdentitySetupMode,
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentPayloadBytes: Map<String, ByteArray> = emptyMap()
): DirectConversationUiState {
    val isChatAuthorized = isDirectChatAuthorized(contact, handshake, setupMode)
    val composerState =
        resolveDirectComposerState(
            hasConversation = conversation != null,
            isChatAuthorized = isChatAuthorized,
            handshake = handshake,
            setupMode = setupMode
        )

    val contactName = resolveContactName(contact, fallbackContactName)
    val conversationMessages = conversation?.messages.orEmpty()
    val messagesById = conversationMessages.associateBy(DirectMessage::id)

    return DirectConversationUiState(
        contactId = contactId,
        contactName = contactName,
        messages =
            buildList {
                for (message in conversationMessages.asReversed()) {
                    add(
                        message.toMessageBubbleUi(
                            safetyAssessments = safetyAssessments,
                            attachmentPayloadBytes = attachmentPayloadBytes,
                            reply = message.replyToMessageId.toDirectReplyPreview(messagesById, contactName)
                        )
                    )
                }
            },
        contactSecurityState = contact.toContactSecurityState(),
        identitySetupMode = setupMode,
        isLoading = contact == null,
        isChatAuthorized = isChatAuthorized,
        composerState = composerState
    )
}

internal fun DirectConversationUiState.withProfilePicture(profilePictureBytes: ByteArray?): DirectConversationUiState =
    copy(profilePictureBytes = profilePictureBytes)
