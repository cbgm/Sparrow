package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupAvatarPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.FindUnpublishedGroupStateUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.groupAdministrationLockKey
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupAvatarUpdatedPacket
import kotlin.reflect.KClass

/**
 * FindUnpublishedAvatar -> AuthorizeAdmin -> CreateAvatarPackets -> EnqueueAvatarPackets -> MarkAvatarPublished.
 * Without an unpublished avatar (published already, or replaced by a newer one from another admin) it stops
 * after FindUnpublishedAvatar.
 *
 * The avatar or its removal was saved locally before; this workflow only sends it. It holds the group administration lock, so the
 * packets are signed for the epoch and recipients that are current when they are queued. The avatar is marked
 * published only while it is still the stored version; a failed send leaves it unpublished for the next attempt.
 */
class PublishGroupAvatarWorkflow internal constructor(
    private val avatarRepository: GroupAvatarRepository,
    private val findUnpublishedGroupState: FindUnpublishedGroupStateUseCase,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val createGroupAvatarPackets: CreateGroupAvatarPacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : WorkflowStateMachine<
        GroupStateCommand.PublishGroupAvatar,
        PublishGroupAvatarWorkflow.Step,
        PublishGroupAvatarWorkflow.Outcome
    > {
    override val commandType: KClass<GroupStateCommand.PublishGroupAvatar> = GroupStateCommand.PublishGroupAvatar::class

    override fun lockKey(command: GroupStateCommand.PublishGroupAvatar): String =
        groupAdministrationLockKey(command.groupId)

    override fun firstStep(command: GroupStateCommand.PublishGroupAvatar): Step = Step.FindUnpublishedAvatar(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.FindUnpublishedAvatar -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                avatarFound(findUnpublishedGroupState.avatar(step.command.groupId).getOrThrow())
            }
            is Step.AuthorizeAdmin ->
                Outcome.AdminAuthorized(
                    step.avatar,
                    requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow()
                )
            is Step.CreateAvatarPackets ->
                Outcome.AvatarPacketsCreated(step.avatar, createGroupAvatarPackets(step.avatar, step.context).getOrThrow())
            is Step.EnqueueAvatarPackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.AvatarPacketsEnqueued(step.avatar)
            }
            is Step.MarkAvatarPublished -> {
                avatarRepository.markPublished(step.command.groupId, step.avatar.changedAtEpochMilliseconds).getOrThrow()
                Outcome.AvatarPublished
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            Outcome.NothingToPublish -> Transition.Complete()
            is Outcome.UnpublishedAvatarFound -> Transition.Next(Step.AuthorizeAdmin(outcome.avatar, step.command))
            is Outcome.AdminAuthorized ->
                Transition.Next(Step.CreateAvatarPackets(outcome.avatar, outcome.context, step.command))
            is Outcome.AvatarPacketsCreated ->
                Transition.Next(Step.EnqueueAvatarPackets(outcome.avatar, outcome.packets, step.command))
            is Outcome.AvatarPacketsEnqueued -> Transition.Next(Step.MarkAvatarPublished(outcome.avatar, step.command))
            Outcome.AvatarPublished -> Transition.Complete()
        }

    private fun avatarFound(avatar: GroupAvatarState?): Outcome =
        avatar?.let(Outcome::UnpublishedAvatarFound) ?: Outcome.NothingToPublish

    sealed interface Step : WorkflowStep {
        val command: GroupStateCommand.PublishGroupAvatar

        data class FindUnpublishedAvatar(
            override val command: GroupStateCommand.PublishGroupAvatar
        ) : Step

        data class AuthorizeAdmin(
            val avatar: GroupAvatarState,
            override val command: GroupStateCommand.PublishGroupAvatar
        ) : Step

        data class CreateAvatarPackets(
            val avatar: GroupAvatarState,
            val context: GroupAdminContext,
            override val command: GroupStateCommand.PublishGroupAvatar
        ) : Step

        data class EnqueueAvatarPackets(
            val avatar: GroupAvatarState,
            val packets: Map<String, GroupAvatarUpdatedPacket>,
            override val command: GroupStateCommand.PublishGroupAvatar
        ) : Step

        data class MarkAvatarPublished(
            val avatar: GroupAvatarState,
            override val command: GroupStateCommand.PublishGroupAvatar
        ) : Step
    }

    sealed interface Outcome {
        data object NothingToPublish : Outcome

        data class UnpublishedAvatarFound(
            val avatar: GroupAvatarState
        ) : Outcome

        data class AdminAuthorized(
            val avatar: GroupAvatarState,
            val context: GroupAdminContext
        ) : Outcome

        data class AvatarPacketsCreated(
            val avatar: GroupAvatarState,
            val packets: Map<String, GroupAvatarUpdatedPacket>
        ) : Outcome

        data class AvatarPacketsEnqueued(
            val avatar: GroupAvatarState
        ) : Outcome

        data object AvatarPublished : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "group avatar"
    }
}
