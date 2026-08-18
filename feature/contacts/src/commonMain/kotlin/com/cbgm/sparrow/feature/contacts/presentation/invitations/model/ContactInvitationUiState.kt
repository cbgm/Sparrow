package com.cbgm.sparrow.feature.contacts.presentation.invitations.model

import com.cbgm.sparrow.feature.contacts.domain.model.PendingContactInvitation

data class ContactInvitationUiState(
    val invitations: List<PendingContactInvitation> = emptyList(),
    val processingInvitationId: String? = null,
    val profilePictures: Map<String, ByteArray?> = emptyMap()
)
