package com.cbgm.sparrow.feature.contacts.presentation.details.components

import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsContactUi
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactPhoneNumberTypeUi
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactPhoneNumberUi
import com.cbgm.sparrow.feature.contacts.presentation.details.model.DeviceContactLinkUi
import com.cbgm.sparrow.feature.contacts.presentation.details.model.SparrowIdentityUi

internal object ContactDetailsPreviewData {
    val phoneNumber = ContactPhoneNumberUi(
        id = "phone",
        value = "+49 123 456789",
        type = ContactPhoneNumberTypeUi.MOBILE,
        label = null
    )
    val identity = SparrowIdentityUi(
        verifiedByMe = false,
        verifiedByContact = false,
        mutualKeyExchange = true,
        signingFingerprint = "01:02:03",
        encryptionFingerprint = "04:05:06"
    )
    val contact = ContactDetailsContactUi(
        id = "contact",
        displayName = "Alex",
        phoneNumbers = listOf(phoneNumber),
        preferredPhoneNumberId = phoneNumber.id,
        deviceContactLinkStatus = DeviceContactLinkUi.LINKED,
        sparrowIdentity = identity
    )
    val safetyNumber = List(16) { "12345" }.joinToString("")
}
