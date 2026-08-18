package com.cbgm.sparrow.feature.contacts.presentation.invitations.mapper

import com.cbgm.sparrow.feature.contacts.domain.model.PendingContactInvitation
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiState

internal fun List<PendingContactInvitation>.toUiState(
    profilePictures: Map<String, ByteArray?>,
    processingInvitationId: String?
): ContactInvitationUiState =
    ContactInvitationUiState(
        invitations = this,
        processingInvitationId = processingInvitationId,
        profilePictures = profilePictures
    )
