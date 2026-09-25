package com.cbgm.sparrow.feature.invite.presentation.model

enum class InvitationTab {
    INCOMING,
    OUTGOING
}

enum class InvitationUiPayloadType {
    DIRECT,
    GROUP
}

enum class InvitationUiDirection {
    INCOMING,
    OUTGOING
}

enum class InvitationUiStatus {
    PENDING,
    DECLINED,
    EXPIRED,
    FAILED
}

data class InvitationUi(
    val invitationId: String,
    val payloadType: InvitationUiPayloadType,
    val payloadId: String,
    val peerId: String,
    val peerDisplayName: String?,
    val peerSecondaryText: String?,
    val direction: InvitationUiDirection,
    val status: InvitationUiStatus,
    val expiresAtEpochMilliseconds: Long,
    val updatedAtEpochMilliseconds: Long,
    val hasUnreadUpdate: Boolean
)

data class InvitationsUiData(
    val incoming: List<InvitationUi> = emptyList(),
    val outgoing: List<InvitationUi> = emptyList()
)

data class InvitationUiState(
    val selectedTab: InvitationTab = InvitationTab.INCOMING,
    val incomingInvitations: List<InvitationUi> = emptyList(),
    val outgoingInvitations: List<InvitationUi> = emptyList(),
    val processingInvitationId: String? = null
) {
    val hasUnreadIncomingUpdates: Boolean
        get() = incomingInvitations.any(InvitationUi::hasUnreadUpdate)

    val hasUnreadOutgoingUpdates: Boolean
        get() = outgoingInvitations.any(InvitationUi::hasUnreadUpdate)

    val selectedInvitations: List<InvitationUi>
        get() =
            when (selectedTab) {
                InvitationTab.INCOMING -> incomingInvitations
                InvitationTab.OUTGOING -> outgoingInvitations
            }
}

/** Additional incoming mailbox requests supplied by the navigation composition layer. */
data class MailboxReviewRequestUi(
    val peerId: String,
    val invitationId: String,
    val peerDisplayName: String,
    val proposedSigningFingerprint: String,
    val proposedEncryptionFingerprint: String
)
