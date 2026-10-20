package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase

import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseBoard
import com.cbgm.sparrow.core.base.messagepart.domain.model.ExpenseMemberId
import com.cbgm.sparrow.core.util.result.safeSuspendCall
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupExpenseMembers
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupPinRepository
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import com.cbgm.sparrow.protocol.identity.LocalPublicIdentityProvider
import kotlinx.coroutines.flow.first

/** Membership's installed epoch keys identify expense members; contact verification is not required. */
class FindGroupExpenseMembersUseCase(
    private val pinRepository: GroupPinRepository,
    private val membershipRepository: GroupMembershipRepository,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider
) {
    suspend operator fun invoke(groupId: String): Result<GroupExpenseMembers> =
        safeSuspendCall(logFailure = false) {
            require(groupId.isNotBlank()) { "Group ID is missing" }
            val board = pinRepository.find(groupId).getOrThrow()?.parts
                ?.filterIsInstance<ExpenseBoard>()
                ?.singleOrNull { it.closedAtEpochMilliseconds == null }
                ?: error("Group expenses are not active")

            val administration = membershipRepository.observeAdministration(groupId).first { it.activeMemberCount > 0 }
            val routingMembers = membershipRepository.getCurrentTransportRoutingMembers(groupId).getOrThrow().orEmpty()
                .associateBy { it.contactId }
            val localId = ExpenseMemberId.fromSigningPublicKey(
                localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow().signingPublicKey
            )
            val remoteIds = administration.currentMemberContactIds.mapNotNull { contactId ->
                routingMembers[contactId]?.signingPublicKey?.let(ExpenseMemberId::fromSigningPublicKey)
            }
            GroupExpenseMembers(
                board = board,
                memberIds = setOf(localId) + remoteIds,
                localMemberId = localId,
                isLocalAdmin = administration.isLocalAdmin
            )
        }
}
