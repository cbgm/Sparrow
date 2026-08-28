package com.cbgm.sparrow.feature.contacts.presentation.invitations.model

import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitation
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityInvitationDirection

enum class ContactInvitationTab(
    val direction: IdentityInvitationDirection
) {
    INCOMING(IdentityInvitationDirection.INCOMING),
    OUTGOING(IdentityInvitationDirection.OUTGOING)
}

data class ContactInvitationUiState(
    val selectedTab: ContactInvitationTab = ContactInvitationTab.INCOMING,
    val incomingInvitations: List<ContactInvitation> = emptyList(),
    val outgoingInvitations: List<ContactInvitation> = emptyList(),
    val processingInvitationId: String? = null,
    val profilePictures: Map<String, ByteArray?> = emptyMap()
) {
    val hasUnreadIncomingUpdates: Boolean
        get() = incomingInvitations.any(ContactInvitation::hasUnreadUpdate)

    val hasUnreadOutgoingUpdates: Boolean
        get() = outgoingInvitations.any(ContactInvitation::hasUnreadUpdate)

    val selectedInvitations: List<ContactInvitation>
        get() =
            when (selectedTab) {
                ContactInvitationTab.INCOMING -> incomingInvitations
                ContactInvitationTab.OUTGOING -> outgoingInvitations
            }
}
