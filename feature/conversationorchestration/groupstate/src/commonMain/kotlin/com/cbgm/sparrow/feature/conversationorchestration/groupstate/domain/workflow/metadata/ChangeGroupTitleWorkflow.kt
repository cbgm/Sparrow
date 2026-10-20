package com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.workflow.metadata

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupAdministrationCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.ConversationWorkflow
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupAdminContext
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.model.GroupTitleState
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.repository.GroupTitleRepository
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.CreateGroupTitlePacketsUseCase
import com.cbgm.sparrow.feature.conversationorchestration.groupstate.domain.usecase.RequireGroupAdminContextUseCase
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.usecase.EnqueueGroupPacketsUseCase
import com.cbgm.sparrow.protocol.packet.GroupTitleUpdatedPacket
import kotlin.reflect.KClass

/** AuthorizeAdmin -> LoadTitle -> SaveTitle -> CreateTitlePackets -> EnqueueTitlePackets. An unchanged title stops after LoadTitle. */
class ChangeGroupTitleWorkflow internal constructor(
    private val titleRepository: GroupTitleRepository,
    private val requireGroupAdminContext: RequireGroupAdminContextUseCase,
    private val createGroupTitlePackets: CreateGroupTitlePacketsUseCase,
    private val enqueueGroupPackets: EnqueueGroupPacketsUseCase
) : ConversationWorkflow<
        GroupAdministrationCommand.ChangeTitle,
        ChangeGroupTitleWorkflow.Step,
        ChangeGroupTitleWorkflow.Outcome
    > {
    override val commandType: KClass<GroupAdministrationCommand.ChangeTitle> =
        GroupAdministrationCommand.ChangeTitle::class

    override fun lockKey(command: GroupAdministrationCommand.ChangeTitle): String =
        groupMetadataLockKey(command.groupId)

    override fun firstStep(command: GroupAdministrationCommand.ChangeTitle): Step = Step.AuthorizeAdmin(command)

    override suspend fun perform(step: Step): Outcome =
        when (step) {
            is Step.AuthorizeAdmin -> {
                require(step.command.groupId.isNotBlank()) { "Group ID must not be blank" }
                require(step.command.normalizedTitle.isNotEmpty()) { "Group title must not be blank" }
                Outcome.AdminAuthorized(requireGroupAdminContext(step.command.groupId, ADMIN_ACTION).getOrThrow())
            }
            is Step.LoadTitle -> {
                val current = titleRepository.get(step.command.groupId).getOrThrow()
                if (current.title == step.command.normalizedTitle) {
                    Outcome.TitleUnchanged
                } else {
                    Outcome.TitleLoaded(current, step.context)
                }
            }
            is Step.SaveTitle -> {
                val title =
                    GroupTitleState(
                        groupId = step.command.groupId,
                        title = step.command.normalizedTitle,
                        changedAtEpochMilliseconds =
                            maxOf(SystemClock.nowEpochMilliseconds(), step.previous.changedAtEpochMilliseconds + 1L)
                    )
                titleRepository.save(title).getOrThrow()
                Outcome.TitleSaved(title, step.context)
            }
            is Step.CreateTitlePackets ->
                Outcome.TitlePacketsCreated(createGroupTitlePackets(step.title, step.context).getOrThrow())
            is Step.EnqueueTitlePackets -> {
                enqueueGroupPackets(step.packets, recordFailedRecipients = false).getOrThrow()
                Outcome.TitlePacketsEnqueued
            }
        }

    override fun transition(step: Step, outcome: Outcome): Transition<Step> =
        when (outcome) {
            is Outcome.AdminAuthorized -> Transition.Next(Step.LoadTitle(outcome.context, step.command))
            Outcome.TitleUnchanged -> Transition.Complete()
            is Outcome.TitleLoaded -> Transition.Next(Step.SaveTitle(outcome.previous, outcome.context, step.command))
            is Outcome.TitleSaved ->
                Transition.Next(Step.CreateTitlePackets(outcome.title, outcome.context, step.command))
            is Outcome.TitlePacketsCreated ->
                Transition.Next(Step.EnqueueTitlePackets(outcome.packets, step.command))
            Outcome.TitlePacketsEnqueued -> Transition.Complete()
        }

    private val GroupAdministrationCommand.ChangeTitle.normalizedTitle: String get() = title.trim()

    sealed interface Step : WorkflowStep {
        val command: GroupAdministrationCommand.ChangeTitle

        data class AuthorizeAdmin(
            override val command: GroupAdministrationCommand.ChangeTitle
        ) : Step

        data class LoadTitle(
            val context: GroupAdminContext,
            override val command: GroupAdministrationCommand.ChangeTitle
        ) : Step

        data class SaveTitle(
            val previous: GroupTitleState,
            val context: GroupAdminContext,
            override val command: GroupAdministrationCommand.ChangeTitle
        ) : Step

        data class CreateTitlePackets(
            val title: GroupTitleState,
            val context: GroupAdminContext,
            override val command: GroupAdministrationCommand.ChangeTitle
        ) : Step

        data class EnqueueTitlePackets(
            val packets: Map<String, GroupTitleUpdatedPacket>,
            override val command: GroupAdministrationCommand.ChangeTitle
        ) : Step
    }

    sealed interface Outcome {
        data class AdminAuthorized(
            val context: GroupAdminContext
        ) : Outcome

        data object TitleUnchanged : Outcome

        data class TitleLoaded(
            val previous: GroupTitleState,
            val context: GroupAdminContext
        ) : Outcome

        data class TitleSaved(
            val title: GroupTitleState,
            val context: GroupAdminContext
        ) : Outcome

        data class TitlePacketsCreated(
            val packets: Map<String, GroupTitleUpdatedPacket>
        ) : Outcome

        data object TitlePacketsEnqueued : Outcome
    }

    private companion object {
        const val ADMIN_ACTION = "group title"
    }
}
