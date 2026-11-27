package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationRole

/**
 * Whether the local member verifies the group as its admin, as a member, or without any security state. A member
 * learns every admin of the current epoch, so it can verify a promoted admin too.
 */
class ResolveGroupVerificationRoleUseCase(
    private val verificationInputs: GroupVerificationInputsUseCase
) {
    suspend operator fun invoke(groupId: String): Result<GroupVerificationRole> =
        safeSuspendCall(logFailure = false) {
            val context = verificationInputs.membershipContext(groupId).getOrThrow()
            val security = context.security
            when {
                security == null -> GroupVerificationRole.WithoutSecurity
                security.isLocalAdmin -> GroupVerificationRole.Admin
                else ->
                    GroupVerificationRole.Member(
                        adminContactId = security.ownerContactId,
                        currentAdminContactIds =
                            context.memberKeys.filter { key -> key.isAdmin }.mapTo(hashSetOf()) { key -> key.contactId }
                    )
            }
        }
}
