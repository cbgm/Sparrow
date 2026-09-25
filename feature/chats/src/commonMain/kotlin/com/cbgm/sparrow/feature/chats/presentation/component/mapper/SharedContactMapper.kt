package com.cbgm.sparrow.feature.chats.presentation.component.mapper

import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactUi

internal fun ContactUi.toSharedContact(): SharedContact? {
    val phoneNumber = preferredPhoneNumber ?: return null
    return SharedContact(
        displayName = displayName?.takeIf(String::isNotBlank),
        phoneNumber = phoneNumber
    )
}
