package com.cbgm.sparrow.feature.contacts.presentation.blocklist.model

import com.cbgm.sparrow.feature.contacts.domain.model.Contact

data class BlockedContactsUiState(
    val blockedContacts: List<Contact> = emptyList(),
    val availableContacts: List<Contact> = emptyList(),
    val profilePictures: Map<String, ByteArray?> = emptyMap(),
    val showAddContacts: Boolean = false,
    val phoneNumber: String = "",
    val phoneNumberError: String? = null,
    val processingContactId: String? = null
)
