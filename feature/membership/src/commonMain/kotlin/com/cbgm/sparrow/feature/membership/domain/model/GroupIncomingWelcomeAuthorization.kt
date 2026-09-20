package com.cbgm.sparrow.feature.membership.domain.model

/** Membership-owned authorization for one received group welcome; no database entities cross modules. */
data class GroupIncomingWelcomeAuthorization(
    val isFirstWelcome: Boolean,
    val sourceInvitationId: String?,
    val previousSigningKeysByContactId: Map<String, ByteArray>,
    val priorAdminEncryptionPublicKey: ByteArray?,
    val priorAdminSigningPublicKey: ByteArray?
)
