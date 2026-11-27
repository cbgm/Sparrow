package com.cbgm.sparrow.feature.membership.domain.usecase

import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.membership.domain.model.StartedMembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository

/** Invites a contact into a group; only a group admin invites. */
class StartGroupMembershipUseCase(
    private val repository: MembershipRepository,
    private val groupMembershipRepository: GroupMembershipRepository
) {
    suspend operator fun invoke(
        groupId: String,
        title: String,
        peerId: String
    ): Result<StartedMembershipHandshake> =
        safeSuspendCall(logFailure = false) {
            val security = groupMembershipRepository.getVerificationMembershipContext(groupId).getOrThrow().security
            check(security?.isLocalAdmin == true) { "Only a group admin can add members" }
            repository.startHandshake(groupId, title, peerId).getOrThrow()
        }
}
