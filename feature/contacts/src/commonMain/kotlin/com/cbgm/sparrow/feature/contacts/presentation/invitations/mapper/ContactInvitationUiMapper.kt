package com.cbgm.sparrow.feature.contacts.presentation.invitations.mapper

import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationDirection
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationStatus
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationTab
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUi
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiState
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationsUiData
import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationsContext

internal fun InvitationsContext.toContactInvitationsUiData(): ContactInvitationsUiData =
    ContactInvitationsUiData(
        incoming = incoming.map(Invitation::toContactInvitationUi),
        outgoing = outgoing.map(Invitation::toContactInvitationUi)
    )

internal fun toContactInvitationUiState(
    selectedTab: ContactInvitationTab,
    invitations: ContactInvitationsUiData,
    processingInvitationId: String?
): ContactInvitationUiState =
    ContactInvitationUiState(
        selectedTab = selectedTab,
        incomingInvitations = invitations.incoming,
        outgoingInvitations = invitations.outgoing,
        processingInvitationId = processingInvitationId
    )

internal fun ContactInvitationTab.toInvitationDirection(): InvitationDirection =
    when (this) {
        ContactInvitationTab.INCOMING -> InvitationDirection.INCOMING
        ContactInvitationTab.OUTGOING -> InvitationDirection.OUTGOING
    }

private fun Invitation.toContactInvitationUi(): ContactInvitationUi =
    ContactInvitationUi(
        invitationId = invitationId,
        contactId = peerId,
        contactName = peerDisplayName,
        contactPhoneNumber = peerSecondaryText,
        direction = direction.toContactInvitationDirection(),
        status = status.toContactInvitationStatus(),
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
        hasUnreadUpdate = hasUnreadUpdate
    )

private fun InvitationDirection.toContactInvitationDirection(): ContactInvitationDirection =
    when (this) {
        InvitationDirection.INCOMING -> ContactInvitationDirection.INCOMING
        InvitationDirection.OUTGOING -> ContactInvitationDirection.OUTGOING
    }

private fun InvitationStatus.toContactInvitationStatus(): ContactInvitationStatus =
    when (this) {
        InvitationStatus.PENDING -> ContactInvitationStatus.PENDING
        InvitationStatus.DECLINED -> ContactInvitationStatus.DECLINED
        InvitationStatus.EXPIRED -> ContactInvitationStatus.EXPIRED
        InvitationStatus.FAILED -> ContactInvitationStatus.FAILED
    }
