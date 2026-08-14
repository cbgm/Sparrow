package com.cbgm.sparrow.feature.contacts.domain.model

data class ContactBlocklist(
    val blockedContacts: List<Contact>,
    val availableContacts: List<Contact>
)
