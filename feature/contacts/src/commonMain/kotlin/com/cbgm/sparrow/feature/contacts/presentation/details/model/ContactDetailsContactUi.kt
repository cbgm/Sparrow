package com.cbgm.sparrow.feature.contacts.presentation.details.model

import androidx.compose.runtime.Immutable

@Immutable
data class ContactDetailsContactUi(
    val id: String,
    val displayName: String?,
    val phoneNumbers: List<ContactPhoneNumberUi>,
    val preferredPhoneNumberId: String?,
    val deviceContactLinkStatus: DeviceContactLinkUi,
    val sparrowIdentity: SparrowIdentityUi?
) {
    val preferredPhoneNumber: String?
        get() = phoneNumbers.firstOrNull { it.id == preferredPhoneNumberId }?.value
            ?: phoneNumbers.firstOrNull()?.value
}

@Immutable
data class ContactPhoneNumberUi(
    val id: String,
    val value: String,
    val type: ContactPhoneNumberTypeUi,
    val label: String?
)

enum class ContactPhoneNumberTypeUi { MOBILE, WORK_MOBILE, HOME, WORK, MAIN, CUSTOM, OTHER }

enum class DeviceContactLinkUi { NOT_LINKED, LINKED, MISSING }

@Immutable
data class SparrowIdentityUi(
    val verifiedByMe: Boolean,
    val verifiedByContact: Boolean,
    val mutualKeyExchange: Boolean,
    val signingFingerprint: String,
    val encryptionFingerprint: String
)
