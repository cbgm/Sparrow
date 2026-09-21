package com.cbgm.sparrow.feature.contacts.presentation.details.mapper

import com.cbgm.sparrow.core.extensions.toFingerprint
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactDetailsContactUi
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactPhoneNumberTypeUi
import com.cbgm.sparrow.feature.contacts.presentation.details.model.ContactPhoneNumberUi
import com.cbgm.sparrow.feature.contacts.presentation.details.model.DeviceContactLinkUi
import com.cbgm.sparrow.feature.contacts.presentation.details.model.SparrowIdentityUi
import com.cbgm.sparrow.feature.identity.domain.model.ContactVerificationStatus
import com.cbgm.sparrow.feature.identity.domain.model.KeyExchangeStatus

internal fun Contact.toContactDetailsUi(): ContactDetailsContactUi = ContactDetailsContactUi(
    id = id,
    displayName = displayName,
    phoneNumbers = phoneNumbers.map { number ->
        ContactPhoneNumberUi(
            id = number.id,
            value = number.value,
            type = when (number.type) {
                ContactPhoneNumberType.MOBILE -> ContactPhoneNumberTypeUi.MOBILE
                ContactPhoneNumberType.WORK_MOBILE -> ContactPhoneNumberTypeUi.WORK_MOBILE
                ContactPhoneNumberType.HOME -> ContactPhoneNumberTypeUi.HOME
                ContactPhoneNumberType.WORK -> ContactPhoneNumberTypeUi.WORK
                ContactPhoneNumberType.MAIN -> ContactPhoneNumberTypeUi.MAIN
                ContactPhoneNumberType.CUSTOM -> ContactPhoneNumberTypeUi.CUSTOM
                ContactPhoneNumberType.OTHER -> ContactPhoneNumberTypeUi.OTHER
            },
            label = number.label
        )
    },
    preferredPhoneNumberId = preferredPhoneNumberId,
    deviceContactLinkStatus = when (deviceContactLinkStatus) {
        DeviceContactLinkStatus.NOT_LINKED -> DeviceContactLinkUi.NOT_LINKED
        DeviceContactLinkStatus.LINKED -> DeviceContactLinkUi.LINKED
        DeviceContactLinkStatus.MISSING -> DeviceContactLinkUi.MISSING
    },
    sparrowIdentity = sparrowIdentity?.let { identity ->
        SparrowIdentityUi(
            verifiedByMe = identity.verificationStatus == ContactVerificationStatus.VERIFIED,
            verifiedByContact = identity.verifiedByContact,
            mutualKeyExchange = identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL,
            signingFingerprint = identity.signingPublicKey.toFingerprint(),
            encryptionFingerprint = identity.encryptionPublicKey.toFingerprint()
        )
    }
)
