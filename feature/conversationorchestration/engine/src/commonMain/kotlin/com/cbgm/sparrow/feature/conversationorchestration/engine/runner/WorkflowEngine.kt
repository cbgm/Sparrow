package com.cbgm.sparrow.feature.conversationorchestration.engine.runner

import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.conversationorchestration.api.command.ConversationCommand
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailure
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureException
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationFailureReason
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationResult
import com.cbgm.sparrow.feature.conversationorchestration.api.model.ConversationState
import com.cbgm.sparrow.feature.conversationorchestration.engine.failure.ConversationFailureMapper
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.Transition
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowCommand
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Executes workflow state machines step by step and publishes their progress.
 *
 * Callers only hand over a command; the [WorkflowRegistry] knows which workflow handles it.
 */
class WorkflowEngine(
    private val locks: WorkflowLocks,
    private val registry: WorkflowRegistry
) {
    private val logger = SparrowLog.withTag("WorkflowEngine")

    /** Executes a command Chats submitted and publishes every step. */
    fun execute(command: ConversationCommand): Flow<ConversationState> = executeCommand(command, acquireLock = true)

    /**
     * Runs a command submitted inside orchestration to its end. A failure keeps its original exception so
     * the caller can react to specific domain errors.
     *
     * @param acquireLock false only when the command runs nested inside a workflow that already holds the
     * same lock key; the lock is not reentrant.
     */
    suspend fun run(command: WorkflowCommand, acquireLock: Boolean = true): Result<ConversationResult> =
        runCommand(command, acquireLock)

    /** Runs a public command nested inside another workflow, e.g. forwarding sends a message. */
    suspend fun run(command: ConversationCommand, acquireLock: Boolean = true): Result<ConversationResult> =
        runCommand(command, acquireLock)

    private suspend fun runCommand(command: Any, acquireLock: Boolean): Result<ConversationResult> =
        when (val finalState = executeCommand(command, acquireLock).first { it !is ConversationState.Running }) {
            is ConversationState.Completed -> Result.success(finalState.result)
            is ConversationState.Failed ->
                Result.failure(finalState.failure.cause ?: ConversationFailureException(finalState.failure))
            is ConversationState.Running -> error("A running state is never final")
        }

    private fun executeCommand(command: Any, acquireLock: Boolean): Flow<ConversationState> {
        val workflow = registry.find(command) ?: return flowOf(ConversationState.Failed(unsupported(command)))
        return flow {
            val lockKey = if (acquireLock) workflow.lockKey(command) else null
            locks.withLock(lockKey) {
                runSteps(workflow, workflow.firstStep(command))
            }
        }.catch { error ->
            if (error is CancellationException) throw error
            logger.warn(error) { "Workflow ${workflow::class.simpleName} failed" }
            emit(ConversationState.Failed(ConversationFailureMapper.toFailure(error)))
        }
    }

    private fun unsupported(command: Any): ConversationFailure =
        ConversationFailure(
            reason = ConversationFailureReason.UNEXPECTED,
            message = "No workflow handles ${command::class.simpleName}"
        )

    private suspend fun <C : Any, S : WorkflowStep, O : Any> FlowCollector<ConversationState>.runSteps(
        workflow: WorkflowStateMachine<C, S, O>,
        firstStep: S
    ) {
        var step = firstStep
        repeat(MAX_STEPS) {
            emit(ConversationState.Running(step.name))
            val outcome = workflow.perform(step)
            when (val transition = workflow.transition(step, outcome)) {
                is Transition.Next -> step = transition.step
                is Transition.Complete -> {
                    emit(ConversationState.Completed(transition.result))
                    return
                }
                is Transition.Reject -> {
                    emit(ConversationState.Failed(transition.failure))
                    return
                }
            }
        }
        error("Workflow ${workflow::class.simpleName} exceeded $MAX_STEPS steps")
    }

    private companion object {
        /**
         * Only stops a transition cycle. Workflows that walk a list take a few steps per item (a read receipt per
         * unread message, a pipeline per queued message), so this is far above any real list.
         */
        const val MAX_STEPS = 100_000
    }
}
