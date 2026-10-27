package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationRole

/** Whether the local member verifies the group as its admin, as a member, or without any security state. */
class ResolveGroupVerificationRoleUseCase(
    private val verificationInputs: GroupVerificationInputsUseCase
) {
    suspend operator fun invoke(groupId: String): Result<GroupVerificationRole> =
        safeSuspendCall(logFailure = false) {
            val security = verificationInputs.membershipContext(groupId).getOrThrow().security
            when {
                security == null -> GroupVerificationRole.WithoutSecurity
                security.isLocalAdmin -> GroupVerificationRole.Admin
                else -> GroupVerificationRole.Member(security.ownerContactId)
            }
        }
}
