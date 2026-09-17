package com.cbgm.sparrow.feature.membership.data

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus

fun Contact.hasMutualGroupIdentity(): Boolean {
    val identity = sparrowIdentity ?: return false
    return identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL &&
        identity.encryptionPublicKey.isNotEmpty() &&
        identity.signingPublicKey.isNotEmpty()
}

fun Contact.groupMembershipDisplayName(): String =
    displayName?.trim()?.takeIf(String::isNotEmpty)
        ?: preferredPhoneNumber?.value?.trim()?.takeIf(String::isNotEmpty)
        ?: "Member"

fun Contact.requireGroupPhoneNumber(): String =
    preferredPhoneNumber?.value?.trim()?.takeIf(String::isNotEmpty)
        ?: phoneNumbers
            .firstOrNull()
            ?.value
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        ?: error("Contact has no phone number: $id")
