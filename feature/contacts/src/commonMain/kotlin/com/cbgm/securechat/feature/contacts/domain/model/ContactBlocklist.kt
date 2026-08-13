package com.cbgm.securechat.feature.contacts.domain.model

data class ContactBlocklist(
    val blockedContacts: List<Contact>,
    val availableContacts: List<Contact>
)
