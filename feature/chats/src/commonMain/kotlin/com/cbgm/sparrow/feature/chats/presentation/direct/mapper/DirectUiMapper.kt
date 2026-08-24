package com.cbgm.sparrow.feature.chats.presentation.direct.mapper

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.chats.domain.model.MessageContentStatus
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessage
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageMediaAttachmentModel
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectComposerState
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectUiState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.sparrow.feature.safety.domain.model.MessageSafetyAssessment
import com.cbgm.sparrow.feature.safety.presentation.details.mapper.toWarningUiModel

internal fun resolveContactName(
    contact: Contact?,
    fallbackContactName: String
): String =
    contact
        ?.displayName
        ?.takeIf(String::isNotBlank)
        ?: fallbackContactName.takeIf(String::isNotBlank)
        ?: "Unknown contact"

internal fun DirectMessage.toUiModel(
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentBytes: Map<String, ByteArray> = emptyMap()
): MessageBubbleModel =
    MessageBubbleModel(
        id = id,
        text = text,
        isMine = isMine,
        security = security,
        contentStatus = contentStatus,
        deliveryStatus = deliveryStatus,
        safetyWarning =
            if (isMine || contentStatus != MessageContentStatus.READABLE) {
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
    )

internal fun Contact?.toSecurityState(): ContactSecurityState {
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

internal fun toDirectUiState(
    contactId: String,
    fallbackContactName: String,
    conversation: DirectConversation?,
    contact: Contact?,
    handshake: IdentityHandshakeState?,
    setupMode: DirectIdentitySetupMode,
    currentText: String,
    currentError: String?,
    contactTyping: Boolean,
    safetyAssessments: Map<String, MessageSafetyAssessment>,
    attachmentBytes: Map<String, ByteArray> = emptyMap()
): DirectUiState {
    val isChatAuthorized = isDirectChatAuthorized(contact, handshake, setupMode)
    val composerState =
        resolveDirectComposerState(
            hasConversation = conversation != null,
            isChatAuthorized = isChatAuthorized,
            handshake = handshake,
            setupMode = setupMode
        )

    return DirectUiState(
        contactId = contactId,
        contactName = resolveContactName(contact, fallbackContactName),
        messages =
            buildList {
                for (message in conversation?.messages.orEmpty().asReversed()) {
                    add(message.toUiModel(safetyAssessments, attachmentBytes))
                }
            },
        messageText = currentText,
        isContactTyping = contactTyping,
        contactSecurityState = contact.toSecurityState(),
        identitySetupMode = setupMode,
        isLoading = contact == null,
        isChatAuthorized = isChatAuthorized,
        composerState = composerState,
        errorMessage = currentError
    )
}

internal fun resolveDirectComposerState(
    hasConversation: Boolean,
    isChatAuthorized: Boolean,
    handshake: IdentityHandshakeState?,
    setupMode: DirectIdentitySetupMode
): DirectComposerState {
    if (!hasConversation) return DirectComposerState.DISABLED

    return when (setupMode) {
        DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING ->
            if (isChatAuthorized) {
                DirectComposerState.READY
            } else {
                DirectComposerState.DISABLED
            }

        DirectIdentitySetupMode.AUTOMATIC_INVITATION ->
            when {
                isChatAuthorized -> DirectComposerState.READY
                handshake == IdentityHandshakeState.INVITE_SENT ->
                    DirectComposerState.REINVITE_PENDING

                handshake in REINVITE_RETRY_STATES ->
                    DirectComposerState.REINVITE_REQUIRED

                else -> DirectComposerState.DISABLED
            }
    }
}

private val REINVITE_RETRY_STATES =
    setOf(
        IdentityHandshakeState.CONVERSATION_DELETED,
        IdentityHandshakeState.DECLINED,
        IdentityHandshakeState.EXPIRED,
        IdentityHandshakeState.FAILED
    )

internal fun DirectUiState.withProfilePicture(profilePictureBytes: ByteArray?): DirectUiState =
    copy(profilePictureBytes = profilePictureBytes)
