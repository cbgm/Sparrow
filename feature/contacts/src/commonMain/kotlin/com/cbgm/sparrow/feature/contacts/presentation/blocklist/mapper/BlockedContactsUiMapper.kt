package com.cbgm.sparrow.feature.contacts.presentation.blocklist.mapper

import com.cbgm.sparrow.feature.contacts.domain.model.ContactBlocklist
import com.cbgm.sparrow.feature.contacts.presentation.blocklist.model.BlockedContactsUiState

internal fun ContactBlocklist.toBlockedContactsUiState(
    profilePictures: Map<String, ByteArray?>,
    showAddContacts: Boolean,
    phoneNumber: String,
    phoneNumberError: String?,
    processingContactId: String?
): BlockedContactsUiState =
    BlockedContactsUiState(
        blockedContacts = blockedContacts,
        availableContacts = availableContacts,
        profilePictures = profilePictures,
        showAddContacts = showAddContacts,
        phoneNumber = phoneNumber,
        phoneNumberError = phoneNumberError,
        processingContactId = processingContactId
    )
