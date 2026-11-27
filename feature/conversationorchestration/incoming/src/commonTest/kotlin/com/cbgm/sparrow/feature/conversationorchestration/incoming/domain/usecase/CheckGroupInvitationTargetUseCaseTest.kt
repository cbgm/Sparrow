package com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.usecase

import com.cbgm.sparrow.feature.conversationorchestration.incoming.domain.workflow.operation.FakeGroupMembershipRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupConversationKind
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantActivation
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantAddition
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantRemoval
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.membership.domain.model.GroupInvitationTarget
import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.usecase.InspectGroupInvitationTargetUseCase
import com.cbgm.sparrow.protocol.handler.IncomingMessageRejectedException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * An invitation stages its conversation before it is accepted, so its group ID must not take over a conversation
 * that is not its own pending invitation.
 */
class CheckGroupInvitationTargetUseCaseTest {
    @Test
    fun invitationIntoANewGroupIsStaged() = runBlocking<Unit> {
        assertTrue(check(GroupConversationKind.NONE, target()).isSuccess)
    }

    @Test
    fun anotherInvitationFromTheSameOwnerIsStagedAgain() = runBlocking<Unit> {
        assertTrue(check(GroupConversationKind.GROUP, target(pending = setOf(OWNER))).isSuccess)
    }

    @Test
    fun invitationAfterLeavingTheGroupIsStaged() = runBlocking<Unit> {
        assertTrue(check(GroupConversationKind.GROUP, target(live = false)).isSuccess)
    }

    @Test
    fun memberCannotRewriteAGroupTheUserBelongsTo() = runBlocking<Unit> {
        assertIs<IncomingMessageRejectedException>(
            check(GroupConversationKind.GROUP, target(live = true)).exceptionOrNull()
        )
    }

    @Test
    fun invitationCannotTakeOverAnotherOwnersPendingInvitation() = runBlocking<Unit> {
        assertIs<IncomingMessageRejectedException>(
            check(GroupConversationKind.GROUP, target(pending = setOf("mallory"))).exceptionOrNull()
        )
    }

    @Test
    fun invitationCannotTurnADirectChatIntoAGroup() = runBlocking<Unit> {
        assertIs<IncomingMessageRejectedException>(check(GroupConversationKind.OTHER, target()).exceptionOrNull())
    }

    private suspend fun check(kind: GroupConversationKind, target: GroupInvitationTarget): Result<Unit> =
        CheckGroupInvitationTargetUseCase(
            conversationRepository = FakeConversations(kind),
            inspectGroupInvitationTarget = InspectGroupInvitationTargetUseCase(
                FakeGroupMembershipRepository(invitationTargets = mutableMapOf(GROUP_ID to target))
            )
        )(offer(), inviterContactId = OWNER)

    private fun target(pending: Set<String> = emptySet(), live: Boolean = false) =
        GroupInvitationTarget(pendingInviterContactIds = pending, hasLiveMembership = live)

    private fun offer() =
        IncomingMembershipOffer(
            sourceId = "invitation-1",
            groupId = GROUP_ID,
            peerId = OWNER,
            title = "Climbing",
            createdAtEpochMilliseconds = 1L,
            expiresAtEpochMilliseconds = 2L,
            ownerEncryptionPublicKey = ByteArray(32) { 1 },
            ownerSigningPublicKey = ByteArray(32) { 2 }
        )

    private class FakeConversations(
        private val kind: GroupConversationKind
    ) : GroupConversationRepository {
        override suspend fun findKind(groupId: String): Result<GroupConversationKind> = Result.success(kind)

        override suspend fun create(title: String): Result<String> = error(NOT_USED)

        override suspend fun getMembershipContext(groupId: String): Result<GroupMembershipContext> = error(NOT_USED)

        override suspend fun isParticipant(groupId: String, contactId: String): Result<Boolean> = error(NOT_USED)

        override suspend fun stageIncoming(
            groupId: String,
            title: String,
            createdAtEpochMilliseconds: Long,
            updatedAtEpochMilliseconds: Long
        ): Result<Boolean> = error(NOT_USED)

        override suspend fun showAcceptedIncoming(groupId: String): Result<Unit> = error(NOT_USED)

        override suspend fun discardPending(groupId: String, updatedAtEpochMilliseconds: Long): Result<Unit> =
            error(NOT_USED)

        override suspend fun addParticipant(addition: GroupParticipantAddition): Result<Unit> = error(NOT_USED)

        override suspend fun promoteParticipant(
            groupId: String,
            contactId: String,
            updatedAtEpochMilliseconds: Long
        ): Result<Unit> = error(NOT_USED)

        override suspend fun removeParticipant(removal: GroupParticipantRemoval): Result<Unit> = error(NOT_USED)

        override suspend fun recordRemoteMemberAdded(activation: GroupParticipantActivation): Result<Unit> =
            error(NOT_USED)

        override suspend fun activateRemoteParticipant(activation: GroupParticipantActivation): Result<Unit> =
            error(NOT_USED)

        override suspend fun endLocalMembership(end: GroupLocalMembershipEnd): Result<Unit> = error(NOT_USED)

        override suspend fun deleteLocalConversation(groupId: String, deletedAtEpochMilliseconds: Long): Result<Unit> =
            error(NOT_USED)
    }

    private companion object {
        const val NOT_USED = "Not used by the invitation target check"
        const val GROUP_ID = "group-1"
        const val OWNER = "owner"
    }
}
