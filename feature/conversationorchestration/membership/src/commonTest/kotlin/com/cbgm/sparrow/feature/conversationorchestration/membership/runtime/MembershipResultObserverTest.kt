package com.cbgm.sparrow.feature.conversationorchestration.membership.runtime

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowLocks
import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowRegistry
import com.cbgm.sparrow.feature.conversationorchestration.membership.domain.command.MembershipCommand
import com.cbgm.sparrow.feature.membership.domain.model.GroupMembershipContext
import com.cbgm.sparrow.feature.membership.domain.model.IncomingMembershipOffer
import com.cbgm.sparrow.feature.membership.domain.model.MembershipDeclineResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.model.MembershipJoinRequest
import com.cbgm.sparrow.feature.membership.domain.model.MembershipPerspective
import com.cbgm.sparrow.feature.membership.domain.model.MembershipResult
import com.cbgm.sparrow.feature.membership.domain.model.MembershipSigningProof
import com.cbgm.sparrow.feature.membership.domain.model.MembershipStatus
import com.cbgm.sparrow.feature.membership.domain.model.StartedMembershipHandshake
import com.cbgm.sparrow.feature.membership.domain.repository.MembershipRepository
import com.cbgm.sparrow.feature.membership.domain.usecase.ObserveMembershipResultsUseCase
import com.cbgm.sparrow.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.sparrow.protocol.packet.GroupInvitePacket
import com.cbgm.sparrow.protocol.packet.GroupInviteReceivedPacket
import com.cbgm.sparrow.protocol.packet.GroupJoinRequestPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

/** A membership result counts as seen only once its workflow succeeded. */
class MembershipResultObserverTest {
    @Test
    fun failedResultIsRetriedUntilItsWorkflowSucceeds() =
        runTest {
            val handler = ResultHandler(failures = 2)
            val results =
                flow {
                    emit(emptyList())
                    emit(listOf(result(MembershipStatus.WELCOME_SENT)))
                }

            observer(results, handler).run()

            assertEquals(listOf(MEMBERSHIP_ID, MEMBERSHIP_ID, MEMBERSHIP_ID), handler.attempts)
        }

    @Test
    fun handledResultIsNotForwardedAgain() =
        runTest {
            val handler = ResultHandler(failures = 0)
            val result = result(MembershipStatus.ACTIVE)
            val results =
                flow {
                    emit(emptyList())
                    emit(listOf(result))
                    emit(listOf(result))
                }

            observer(results, handler).run()

            assertEquals(listOf(MEMBERSHIP_ID), handler.attempts)
        }

    @Test
    fun resultAtStartIsResumedOnlyWhenTheOwnerHasWorkLeft() =
        runTest {
            val handler = ResultHandler(failures = 1)
            val results =
                flow {
                    emit(
                        listOf(
                            result(MembershipStatus.IDENTITY_READY),
                            result(MembershipStatus.FAILED).copy(membershipId = "finished")
                        )
                    )
                }

            observer(results, handler).run()

            assertEquals(listOf(MEMBERSHIP_ID, MEMBERSHIP_ID), handler.attempts)
        }

    private fun observer(results: Flow<List<MembershipResult>>, handler: ResultHandler) =
        MembershipResultObserver(
            ObserveMembershipResultsUseCase(Results(results)),
            WorkflowEngine(WorkflowLocks(), WorkflowRegistry { listOf(handler) })
        )

    private fun result(status: MembershipStatus) =
        MembershipResult(
            membershipId = MEMBERSHIP_ID,
            sourceInvitationId = "invitation",
            groupId = "group",
            peerId = "peer",
            perspective = MembershipPerspective.OWNER,
            status = status,
            updatedAtEpochMilliseconds = 1
        )

    /** Fails the first [failures] attempts. */
    private class ResultHandler(
        private var failures: Int
    ) : WorkflowStateMachine<MembershipCommand.HandleMembershipResult, ResultHandler.Handle, Unit> {
        val attempts = mutableListOf<String>()

        override val commandType: KClass<MembershipCommand.HandleMembershipResult> =
            MembershipCommand.HandleMembershipResult::class

        override fun firstStep(command: MembershipCommand.HandleMembershipResult) =
            Handle(command.result.membershipId)

        override suspend fun perform(step: Handle) {
            attempts += step.membershipId
            if (failures > 0) {
                failures -= 1
                error("database is busy")
            }
        }

        override fun transition(step: Handle, outcome: Unit): Transition<Handle> = Transition.Complete()

        data class Handle(
            val membershipId: String
        ) : WorkflowStep
    }

    private class Results(
        private val results: Flow<List<MembershipResult>>
    ) : MembershipRepository {
        override fun observeResults(): Flow<List<MembershipResult>> = results

        override suspend fun startHandshake(
            groupId: String,
            title: String,
            peerId: String
        ): Result<StartedMembershipHandshake> = error(NOT_USED)

        override suspend fun inspectIncomingOffer(
            peerId: String,
            packet: GroupInvitePacket
        ): Result<IncomingMembershipOffer> = error(NOT_USED)

        override suspend fun receiveIncomingOffer(
            peerId: String,
            packet: GroupInvitePacket,
            receivedAtEpochMilliseconds: Long,
            shouldStage: Boolean
        ): Result<Unit> = error(NOT_USED)

        override suspend fun discardSupersededIncomingHandshakes(peerId: String, currentSourceId: String): Result<Unit> =
            error(NOT_USED)

        override suspend fun getHandshake(sourceId: String): Result<MembershipHandshake?> = error(NOT_USED)

        override suspend fun acceptHandshake(sourceId: String): Result<Unit> = error(NOT_USED)

        override suspend fun declineHandshake(sourceId: String): Result<Unit> = error(NOT_USED)

        override suspend fun receiveOfferReceipt(
            peerId: String,
            packet: GroupInviteReceivedPacket
        ): Result<MembershipSigningProof?> = error(NOT_USED)

        override suspend fun receiveDecline(
            peerId: String,
            packet: GroupInviteDeclinedPacket
        ): Result<MembershipDeclineResult?> = error(NOT_USED)

        override suspend fun receiveJoinRequest(
            peerId: String,
            packet: GroupJoinRequestPacket
        ): Result<MembershipJoinRequest?> = error(NOT_USED)

        override suspend fun confirmJoinIdentity(
            sourceId: String,
            updatedAtEpochMilliseconds: Long,
            context: GroupMembershipContext,
            memberEncryptionPublicKey: ByteArray,
            memberSigningPublicKey: ByteArray,
            memberPhoneNumber: String
        ): Result<Unit> = error(NOT_USED)

        override suspend fun markRemoved(sourceId: String, updatedAtEpochMilliseconds: Long): Result<Unit> =
            error(NOT_USED)

        override suspend fun clearHandshake(sourceId: String): Result<Unit> = error(NOT_USED)
    }

    private companion object {
        const val NOT_USED = "Not used"
        const val MEMBERSHIP_ID = "membership"
    }
}
