package com.cbgm.sparrow.feature.chats.presentation.component.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.contacts.domain.model.Contact

internal fun Contact.toSharedContact(): SharedContact? {
    val phoneNumber = preferredPhoneNumber?.value ?: return null
    return SharedContact(
        displayName = displayName?.takeIf(String::isNotBlank),
        phoneNumber = phoneNumber
    )
}
