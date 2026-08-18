package com.cbgm.sparrow.feature.chats.presentation.direct.mapper

import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.feature.chats.domain.model.direct.ContactSecurityState
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessage
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleModel
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectUiState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.contacts.domain.model.KeyExchangeStatus

internal fun resolveContactName(
    contact: Contact?,
    fallbackContactName: String
): String =
    contact
        ?.displayName
        ?.takeIf(String::isNotBlank)
        ?: fallbackContactName.takeIf(String::isNotBlank)
        ?: "Unknown contact"

internal fun DirectMessage.toUiModel(): MessageBubbleModel =
    MessageBubbleModel(
        id = id,
        text = text,
        isMine = isMine,
        security = security,
        contentStatus = contentStatus,
        deliveryStatus = deliveryStatus
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
            identityHandshakeState == IdentityHandshakeState.ACCEPTANCE_SENT ||
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
    contactTyping: Boolean
): DirectUiState =
    DirectUiState(
        contactId = contactId,
        contactName = resolveContactName(contact, fallbackContactName),
        messages = conversation?.messages.orEmpty().asReversed().map { it.toUiModel() },
        messageText = currentText,
        isContactTyping = contactTyping,
        contactSecurityState = contact.toSecurityState(),
        identityHandshakeState = handshake,
        identitySetupMode = setupMode,
        isLoading = contact == null,
        isMessageInputEnabled = isDirectChatAuthorized(contact, handshake, setupMode),
        errorMessage = currentError
    )

internal fun DirectUiState.withProfilePicture(profilePictureBytes: ByteArray?): DirectUiState =
    copy(profilePictureBytes = profilePictureBytes)
