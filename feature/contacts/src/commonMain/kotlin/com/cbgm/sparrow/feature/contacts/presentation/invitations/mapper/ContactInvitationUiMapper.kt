package com.cbgm.sparrow.feature.contacts.presentation.invitations.mapper

import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitation
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationTab
import com.cbgm.sparrow.feature.contacts.presentation.invitations.model.ContactInvitationUiState

internal fun toContactInvitationUiState(
    selectedTab: ContactInvitationTab,
    incomingInvitations: List<ContactInvitation>,
    outgoingInvitations: List<ContactInvitation>,
    profilePictures: Map<String, ByteArray?>,
    processingInvitationId: String?
): ContactInvitationUiState =
    ContactInvitationUiState(
        selectedTab = selectedTab,
        incomingInvitations = incomingInvitations,
        outgoingInvitations = outgoingInvitations,
        processingInvitationId = processingInvitationId,
        profilePictures = profilePictures
    )
