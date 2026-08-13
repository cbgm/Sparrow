package com.cbgm.securechat.feature.chats.data.group.membership

import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus

internal fun Contact.hasMutualGroupIdentity(): Boolean {
    val identity = secureChatIdentity ?: return false
    return identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL &&
        identity.encryptionPublicKey.isNotEmpty() &&
        identity.signingPublicKey.isNotEmpty()
}

internal fun Contact.groupMembershipDisplayName(): String =
    displayName?.trim()?.takeIf(String::isNotEmpty)
        ?: preferredPhoneNumber?.value?.trim()?.takeIf(String::isNotEmpty)
        ?: "Member"

internal fun Contact.requireGroupPhoneNumber(): String =
    preferredPhoneNumber?.value?.trim()?.takeIf(String::isNotEmpty)
        ?: phoneNumbers
            .firstOrNull()
            ?.value
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        ?: error("Contact has no phone number: $id")
