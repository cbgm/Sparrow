package com.cbgm.sparrow.feature.contacts.domain.model

data class ContactInvitationsContext(
    val incoming: List<ContactInvitation> = emptyList(),
    val outgoing: List<ContactInvitation> = emptyList(),
    val profilePictures: Map<String, ByteArray?> = emptyMap()
)
