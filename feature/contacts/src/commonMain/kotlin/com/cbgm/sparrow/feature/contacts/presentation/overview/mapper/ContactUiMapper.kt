package com.cbgm.sparrow.feature.contacts.presentation.overview.mapper

import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactUi

fun Contact.toContactUi(): ContactUi = ContactUi(
    id = id,
    displayName = displayName,
    preferredPhoneNumber = preferredPhoneNumber?.value,
    phoneNumbers = phoneNumbers.map { it.value },
    hasSparrowIdentity = sparrowIdentity != null,
    deviceContactMissing = deviceContactLinkStatus == DeviceContactLinkStatus.MISSING
)

fun List<Contact>.toContactsUi(): List<ContactUi> = map(Contact::toContactUi)
