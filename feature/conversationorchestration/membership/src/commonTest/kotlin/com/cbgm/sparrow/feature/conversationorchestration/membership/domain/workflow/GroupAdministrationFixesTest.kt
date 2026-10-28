package com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow

import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowLocks
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowRegistry
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupConversationKind
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantActivation
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantAddition
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupParticipantRemoval
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationRole
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.model.GroupVerificationRow
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupConversationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.repository.GroupVerificationRepository
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.GroupVerificationInputsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.RefreshOwnedGroupVerificationUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.usecase.ResolveGroupVerificationRoleUseCase
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.workflow.administration.CreateGroupWorkflow
import com.cbgm.sparrow.feature.identity.domain.model.RemotePeerIdentity
import com.cbgm.sparrow.feature.identity.domain.repository.RemoteIdentityReadRepository
import com.cbgm.sparrow.feature.identity.domain.usecase.GetRemoteIdentityUseCase
import com.cbgm.sparrow.feature.membership.domain.model.GroupLocalMembershipEnd
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMemberKey
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.GroupVerificationSecurityState
import com.cbgm.sparrow.feature.membership.domain.repository.GroupMembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A created group is reported even when an invitation fails, so a retry does not create a second group; a
 * member may verify every current admin, also one promoted after its welcome.
 */
class GroupAdministrationFixesTest {
    private var context =
        GroupVerificationMembershipContext(security = null, memberKeys = emptyList(), memberships = emptyList())
    private val memberships =
        object : GroupMembershipRepository by UnusedGroupMembership {
            override suspend fun getVerificationMembershipContext(
                groupId: String
            ): Result<GroupVerificationMembershipContext> = Result.success(context)
        }
    private val inputs =
        GroupVerificationInputsUseCase(memberships, UnusedContacts, GetRemoteIdentityUseCase(UnusedIdentities))

    @Test
    fun failedInvitationStillReportsTheCreatedGroupAndInvitesTheOthers() = runBlocking {
        val invited = mutableListOf<String>()
        val engine = WorkflowEngine(WorkflowLocks(), WorkflowRegistry { listOf(AddMembersWorkflow(invited)) })
        val workflow =
            CreateGroupWorkflow(
                engine = engine,
                conversationRepository = UnusedConversations,
                groupSecurityRepository = UnusedGroupSecurity,
                localSigningKeyPairProvider = UnusedSigningKeys,
                refreshOwnedGroupVerification = RefreshOwnedGroupVerificationUseCase(UnusedVerificationRows, inputs)
            )
        val command = GroupAdministrationCommand.CreateGroup("Trip", linkedSetOf("alice", FAILING_CONTACT, "carol"))

        var transition =
            workflow.transition(
                CreateGroupWorkflow.Step.InitializeVerification(GROUP_ID, command),
                CreateGroupWorkflow.Outcome.VerificationInitialized(GROUP_ID)
            )
        while (transition is Transition.Next) {
            val step = transition.step
            transition = workflow.transition(step, workflow.perform(step))
        }

        val created = ConversationResult.GroupCreated(GROUP_ID, failedContactIds = listOf(FAILING_CONTACT))
        assertEquals(Transition.Complete(created), transition)
        assertEquals(listOf("alice", "carol"), invited)
    }

    @Test
    fun memberMayVerifyEveryCurrentAdminButNoOrdinaryMember() = runBlocking {
        context =
            GroupVerificationMembershipContext(
                security = GroupVerificationSecurityState(ownerContactId = "owner", isLocalAdmin = false),
                memberKeys =
                    listOf(key("owner", isAdmin = true), key("promoted", isAdmin = true), key("bob", isAdmin = false)),
                memberships = emptyList()
            )

        val role = ResolveGroupVerificationRoleUseCase(inputs)(GROUP_ID).getOrThrow()

        assertTrue(role is GroupVerificationRole.Member)
        assertEquals("owner", role.adminContactId)
        assertTrue(role.isAdmin("owner"))
        assertTrue(role.isAdmin("promoted"))
        assertFalse(role.isAdmin("bob"))
    }

    private fun key(contactId: String, isAdmin: Boolean) =
        GroupVerificationMemberKey(contactId, ByteArray(32) { 1 }, ByteArray(32) { 2 }, isAdmin)

    /** Invites one contact; [FAILING_CONTACT] fails like a contact without usable keys. */
    private class AddMembersWorkflow(
        private val invited: MutableList<String>
    ) : WorkflowStateMachine<GroupAdministrationCommand.AddMembers, AddMembersWorkflow.Invite, Unit> {
        override val commandType: KClass<GroupAdministrationCommand.AddMembers> =
            GroupAdministrationCommand.AddMembers::class

        override fun firstStep(command: GroupAdministrationCommand.AddMembers): Invite = Invite(command)

        override suspend fun perform(step: Invite) {
            val contactId = step.command.contactIds.single()
            check(contactId != FAILING_CONTACT) { "Contact has no accepted identity" }
            invited += contactId
        }

        override fun transition(step: Invite, outcome: Unit): Transition<Invite> = Transition.Complete()

        class Invite(
            val command: GroupAdministrationCommand.AddMembers
        ) : WorkflowStep
    }

    private object UnusedIdentities : RemoteIdentityReadRepository {
        override suspend fun get(peerId: String): Result<RemotePeerIdentity?> = error(NOT_USED)

        override suspend fun findPeerIdBySigningPublicKey(signingPublicKey: ByteArray): Result<String?> =
            error(NOT_USED)

        override fun observeAll(): Flow<List<RemotePeerIdentity>> = error(NOT_USED)
    }

    private object UnusedVerificationRows : GroupVerificationRepository {
        override suspend fun findRows(groupId: String): Result<List<GroupVerificationRow>> = error(NOT_USED)

        override suspend fun findRow(groupId: String, invitationId: String): Result<GroupVerificationRow?> =
            error(NOT_USED)

        override suspend fun findLatestUpdatedAt(groupId: String): Result<Long?> = error(NOT_USED)

        override suspend fun replaceGroup(groupId: String, rows: List<GroupVerificationRow>): Result<Unit> =
            error(NOT_USED)

        override suspend fun markAdminVerifiedParticipant(
            groupId: String,
            invitationId: String,
            updatedAtEpochMilliseconds: Long
        ): Result<Boolean> = error(NOT_USED)

        override suspend fun markParticipantVerifiedAdmin(
            groupId: String,
            invitationId: String,
            updatedAtEpochMilliseconds: Long
        ): Result<Boolean> = error(NOT_USED)
    }

    private object UnusedConversations : GroupConversationRepository {
        override suspend fun create(title: String): Result<String> = error(NOT_USED)

        override suspend fun getMembershipContext(groupId: String): Result<GroupMembershipContext> = error(NOT_USED)

        override suspend fun findKind(groupId: String): Result<GroupConversationKind> = error(NOT_USED)

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
        const val NOT_USED = "Not used by the group administration fixes"
        const val GROUP_ID = "group-1"
        const val FAILING_CONTACT = "bob"
    }
}
