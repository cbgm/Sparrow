package com.cbgm.securechat.feature.contacts.presentation.model

import com.cbgm.securechat.feature.contacts.domain.model.Contact

data class BlockedContactsUiState(
    val blockedContacts: List<Contact> = emptyList(),
    val availableContacts: List<Contact> = emptyList(),
    val showAddContacts: Boolean = false,
    val phoneNumber: String = "",
    val phoneNumberError: String? = null,
    val processingContactId: String? = null
)
