package com.cbgm.sparrow.feature.invite.domain.model

data class ContactInvitationsContext(
    val incoming: List<ContactInvitation> = emptyList(),
    val outgoing: List<ContactInvitation> = emptyList()
)
