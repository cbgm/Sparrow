package com.cbgm.securechat.feature.contacts.presentation.screen.details.component

import com.cbgm.securechat.core.crypto.safety.SafetyNumber
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity

internal object ContactDetailsPreviewData {
    val phoneNumber =
        ContactPhoneNumber(
            id = "phone",
            value = "+49 123 456789",
            type = ContactPhoneNumberType.MOBILE,
            label = null
        )

    val identity =
        SecureChatIdentity(
            signingPublicKey = byteArrayOf(1, 2, 3),
            encryptionPublicKey = byteArrayOf(4, 5, 6),
            verificationStatus = ContactVerificationStatus.UNVERIFIED,
            keyExchangeStatus = KeyExchangeStatus.MUTUAL,
            updatedAtEpochMilliseconds = 0L
        )

    val contact =
        Contact(
            id = "contact",
            displayName = "Alex",
            phoneNumbers = listOf(phoneNumber),
            preferredPhoneNumberId = phoneNumber.id,
            deviceContactId = "device-contact",
            deviceContactLinkStatus = DeviceContactLinkStatus.LINKED,
            secureChatIdentity = identity,
            createdAtEpochMilliseconds = 0L,
            updatedAtEpochMilliseconds = 0L
        )

    val safetyNumber =
        SafetyNumber(
            groups = List(16) { "12345" }
        )
}
