package com.cbgm.sparrow.feature.identity.presentation.setup.model

/** UI-safe immutable rendering data. No domain models or mutable public-key buffers reach composables. */
data class PendingIdentityReviewUiState(
    val requests: List<PendingIdentityReviewUi> = emptyList(),
    val dismissingInvitationId: String? = null,
    val confirmingInvitationId: String? = null,
    val errorMessage: String? = null
)

data class PendingIdentityReviewUi(
    val peerId: String,
    val invitationId: String,
    val previousSigningKey: String?,
    val proposedSigningKey: String,
    val previousEncryptionKey: String?,
    val proposedEncryptionKey: String,
    val fingerprintConfirmed: Boolean
)
