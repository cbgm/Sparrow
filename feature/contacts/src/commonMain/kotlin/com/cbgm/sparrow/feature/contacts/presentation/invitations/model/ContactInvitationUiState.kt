package com.cbgm.sparrow.feature.contacts.presentation.invitations.model

enum class ContactInvitationTab {
    INCOMING,
    OUTGOING
}

enum class ContactInvitationDirection {
    INCOMING,
    OUTGOING
}

enum class ContactInvitationStatus {
    PENDING,
    DECLINED,
    EXPIRED,
    FAILED
}

data class ContactInvitationUi(
    val invitationId: String,
    val contactId: String,
    val contactName: String?,
    val contactPhoneNumber: String?,
    val direction: ContactInvitationDirection,
    val status: ContactInvitationStatus,
    val expiresAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long,
    val hasUnreadUpdate: Boolean
)

data class ContactInvitationsUiData(
    val incoming: List<ContactInvitationUi> = emptyList(),
    val outgoing: List<ContactInvitationUi> = emptyList()
)

data class ContactInvitationUiState(
    val selectedTab: ContactInvitationTab = ContactInvitationTab.INCOMING,
    val incomingInvitations: List<ContactInvitationUi> = emptyList(),
    val outgoingInvitations: List<ContactInvitationUi> = emptyList(),
    val processingInvitationId: String? = null
) {
    val hasUnreadIncomingUpdates: Boolean
        get() = incomingInvitations.any(ContactInvitationUi::hasUnreadUpdate)

    val hasUnreadOutgoingUpdates: Boolean
        get() = outgoingInvitations.any(ContactInvitationUi::hasUnreadUpdate)

    val selectedInvitations: List<ContactInvitationUi>
        get() =
            when (selectedTab) {
                ContactInvitationTab.INCOMING -> incomingInvitations
                ContactInvitationTab.OUTGOING -> outgoingInvitations
            }
}
