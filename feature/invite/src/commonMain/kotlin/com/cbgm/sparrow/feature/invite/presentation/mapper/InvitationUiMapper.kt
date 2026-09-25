package com.cbgm.sparrow.feature.invite.presentation.mapper

import com.cbgm.sparrow.feature.invite.domain.model.Invitation
import com.cbgm.sparrow.feature.invite.domain.model.InvitationDirection
import com.cbgm.sparrow.feature.invite.domain.model.InvitationPayloadType
import com.cbgm.sparrow.feature.invite.domain.model.InvitationStatus
import com.cbgm.sparrow.feature.invite.domain.model.InvitationsContext
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationTab
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationUi
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationUiDirection
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationUiPayloadType
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationUiState
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationUiStatus
import com.cbgm.sparrow.feature.invite.presentation.model.InvitationsUiData

internal fun InvitationsContext.toInvitationsUiData(): InvitationsUiData =
    InvitationsUiData(
        incoming = incoming.map(Invitation::toInvitationUi),
        outgoing = outgoing.map(Invitation::toInvitationUi)
    )

internal fun toInvitationUiState(
    selectedTab: InvitationTab,
    invitations: InvitationsUiData,
    processingInvitationId: String?
): InvitationUiState =
    InvitationUiState(
        selectedTab = selectedTab,
        incomingInvitations = invitations.incoming,
        outgoingInvitations = invitations.outgoing,
        processingInvitationId = processingInvitationId
    )

internal fun InvitationTab.toInvitationDirection(): InvitationDirection =
    when (this) {
        InvitationTab.INCOMING -> InvitationDirection.INCOMING
        InvitationTab.OUTGOING -> InvitationDirection.OUTGOING
    }

private fun Invitation.toInvitationUi(): InvitationUi =
    InvitationUi(
        invitationId = invitationId,
        payloadType = payloadType.toInvitationUiPayloadType(),
        payloadId = payloadId,
        peerId = peerId,
        peerDisplayName = peerDisplayName,
        peerSecondaryText = peerSecondaryText,
        direction = direction.toInvitationUiDirection(),
        status = status.toInvitationUiStatus(),
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
        hasUnreadUpdate = hasUnreadUpdate
    )

private fun InvitationDirection.toInvitationUiDirection(): InvitationUiDirection =
    when (this) {
        InvitationDirection.INCOMING -> InvitationUiDirection.INCOMING
        InvitationDirection.OUTGOING -> InvitationUiDirection.OUTGOING
    }

private fun InvitationStatus.toInvitationUiStatus(): InvitationUiStatus =
    when (this) {
        InvitationStatus.PENDING -> InvitationUiStatus.PENDING
        InvitationStatus.DECLINED -> InvitationUiStatus.DECLINED
        InvitationStatus.EXPIRED -> InvitationUiStatus.EXPIRED
        InvitationStatus.FAILED -> InvitationUiStatus.FAILED
    }

private fun InvitationPayloadType.toInvitationUiPayloadType(): InvitationUiPayloadType =
    when (this) {
        InvitationPayloadType.DIRECT -> InvitationUiPayloadType.DIRECT
        InvitationPayloadType.GROUP -> InvitationUiPayloadType.GROUP
    }
