package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupVerificationContext
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupVerificationRepository
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Domain-level composition: Chats owns verification rows; Membership owns membership/security state. */
class ObserveGroupVerificationContextUseCase(
    private val verificationRepository: GroupVerificationRepository,
    private val membershipRepository: GroupMembershipRepository
) {
    operator fun invoke(groupId: String): Flow<GroupVerificationContext> =
        combine(
            verificationRepository.observePairs(groupId),
            membershipRepository.observeVerificationSnapshot(groupId)
        ) { pairs, membership ->
            // Preserve the existing fallback for a group whose local security state has
            // not yet been persisted but whose verification or owner membership is present.
            val isLocalAdmin = membership.isSecurityAdmin ||
                (
                    !membership.hasSecurityState &&
                        (pairs.any { pair -> pair.contactId != null } || membership.hasOwnerMembership)
                )
            GroupVerificationContext(
                hasSecurityState = membership.hasSecurityState,
                isLocalMemberActive = isLocalAdmin || membership.isSecurityMemberActive,
                isLocalAdmin = isLocalAdmin,
                ownerContactId = if (isLocalAdmin) {
                    null
                } else {
                    membership.securityOwnerContactId ?: membership.memberContactId
                },
                ownInvitationId = if (isLocalAdmin) null else membership.memberInvitationId,
                isLeavePending = !isLocalAdmin && membership.isMemberLeavePending
            )
        }
}
