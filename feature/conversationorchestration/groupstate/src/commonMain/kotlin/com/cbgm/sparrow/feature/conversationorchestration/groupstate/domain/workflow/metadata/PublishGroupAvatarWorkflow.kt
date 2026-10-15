package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.core.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.command.GroupStateCommand
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAvatarState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupAvatarRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupAvatarPacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupAvatarUpdatedPacket
import kotlin.reflect.KClass

/** AuthorizeAdmin -> LoadAvatar -> SaveAvatar -> CreateAvatarPackets -> EnqueueAvatarPackets. */
class PublishGroupAvatarWorkflow internal constructor(
    private val avatarRepository: GroupAvatarRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val createGroupAvatarPackets: CreateGroupAvatarPacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : WorkflowStateMachine<
        GroupStateCommand.PublishGroupAvatar,
        PublishGroupAvatarWorkflow.Step,
        PublishGroupAvatarWorkflow.Outcome
    > {
    override val commandType: KClass<GroupStateCommand.PublishGroupAvatar> = GroupStateCommand.PublishGroupAvatar::class

    override fun lockKey(command: GroupStateCommand.PublishGroupAvatar): String = groupMetadataLockKey(command.groupId)

    override fun firstStep(command: GroupStateCommand.PublishGroupAvatar): Step = Step.AuthorizeAdmin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AuthorizeAdmin -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                Outcome.AdminAuthorized(requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow())
            }
            is Step.LoadAvatar ->
                Outcome.AvatarLoaded(avatarRepository.get(step.command.groupId).getOrThrow(), step.context)
            is Step.SaveAvatar -> {
                val avatar =
                    GroupAvatarState(
                        groupId = step.command.groupId,
                        bytes = step.command.bytes,
                        changedAtEpochMilliseconds =
                            maxOf(SystemClock.nowEpochMilliseconds(), step.previous.changedAtEpochMilliseconds + 1L)
                    )
                avatarRepository.save(avatar).getOrThrow()
                Outcome.AvatarSaved(avatar, step.context)
            }
            is Step.CreateAvatarPackets ->
                Outcome.AvatarPacketsCreated(createGroupAvatarPackets(step.avatar, step.context).getOrThrow())
            is Step.EnqueueAvatarPackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.AvatarPacketsEnqueued
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.AdminAuthorized -> Transition.Next(Step.LoadAvatar(outcome.context, step.command))
            is Outcome.AvatarLoaded -> Transition.Next(Step.SaveAvatar(outcome.previous, outcome.context, step.command))
            is Outcome.AvatarSaved ->
                Transition.Next(Step.CreateAvatarPackets(outcome.avatar, outcome.context, step.command))
            is Outcome.AvatarPacketsCreated -> Transition.Next(Step.EnqueueAvatarPackets(outcome.packets, step.command))
            Outcome.AvatarPacketsEnqueued -> Transition.Complete()
        }

    sealed interface Step : WorkflowStep {
        val command: GroupStateCommand.PublishGroupAvatar

        data class AuthorizeAdmin(
            override val command: GroupStateCommand.PublishGroupAvatar
        ) : Step

        data class LoadAvatar(
            val context: GroupAdminContext,
            override val command: GroupStateCommand.PublishGroupAvatar
        ) : Step

        data class SaveAvatar(
            val previous: GroupAvatarState,
            val context: GroupAdminContext,
            override val command: GroupStateCommand.PublishGroupAvatar
        ) : Step

        data class CreateAvatarPackets(
            val avatar: GroupAvatarState,
            val context: GroupAdminContext,
            override val command: GroupStateCommand.PublishGroupAvatar
        ) : Step

        data class EnqueueAvatarPackets(
            val packets: Map<String, GroupAvatarUpdatedPacket>,
            override val command: GroupStateCommand.PublishGroupAvatar
        ) : Step
    }

    sealed interface Outcome {
        data class AdminAuthorized(
            val context: GroupAdminContext
        ) : Outcome

        data class AvatarLoaded(
            val previous: GroupAvatarState,
            val context: GroupAdminContext
        ) : Outcome

        data class AvatarSaved(
            val avatar: GroupAvatarState,
            val context: GroupAdminContext
        ) : Outcome

        data class AvatarPacketsCreated(
            val packets: Map<String, GroupAvatarUpdatedPacket>
        ) : Outcome

        data object AvatarPacketsEnqueued : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "group avatar"
    }
}
