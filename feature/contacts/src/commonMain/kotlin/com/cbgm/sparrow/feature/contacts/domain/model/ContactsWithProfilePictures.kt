package com.cbgm.sparrow.feature.contacts.domain.model

data class ContactsWithProfilePictures(
    val contacts: List<Contact>,
    val profilePictures: Map<String, ByteArray?>
)
