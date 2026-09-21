package com.cbgm.sparrow.feature.contacts.presentation.blocklist.model

import com.cbgm.sparrow.feature.contacts.presentation.overview.model.ContactUi

data class BlockedContactsUiState(
    val blockedContacts: List<ContactUi> = emptyList(),
    val availableContacts: List<ContactUi> = emptyList(),
    val showAddContacts: Boolean = false,
    val phoneNumber: String = "",
    val phoneNumberError: String? = null,
    val processingContactId: String? = null
)
