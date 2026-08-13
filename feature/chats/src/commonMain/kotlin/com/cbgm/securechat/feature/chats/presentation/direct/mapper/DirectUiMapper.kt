package com.cbgm.securechat.feature.chats.presentation.direct.mapper

import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.feature.chats.domain.model.ContactSecurityState
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus

internal fun resolveContactName(
    contact: Contact?,
    conversation: Conversation?,
    fallbackContactName: String
): String =
    contact
        ?.displayName
        ?.takeIf(String::isNotBlank)
        ?: conversation
            ?.contactName
            ?.takeIf(String::isNotBlank)
        ?: fallbackContactName.takeIf(String::isNotBlank)
        ?: "Unknown contact"

internal fun Contact?.toSecurityState(): ContactSecurityState {
    val identity = this?.secureChatIdentity ?: return ContactSecurityState.NO_REMOTE_PUBLIC_KEYS

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
            identityHandshakeState == IdentityHandshakeState.MUTUAL_UNVERIFIED

        DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING ->
            contact?.secureChatIdentity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL
    }
