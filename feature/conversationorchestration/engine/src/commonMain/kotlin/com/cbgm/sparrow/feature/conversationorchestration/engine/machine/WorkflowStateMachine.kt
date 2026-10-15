package com.cbgm.sparrow.feature.conversationorchestration.engine.machine

import kotlin.reflect.KClass

/**
 * A workflow modelled as a state machine.
 *
 * - [perform] executes the single operation that belongs to a step and reports its outcome.
 *   It never decides what happens next.
 * - [transition] decides, without side effects, which step follows that outcome.
 *
 * The [com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine] alternates
 * both until the machine completes or rejects.
 */
interface WorkflowStateMachine<C : Any, S : WorkflowStep, O : Any> {
    val commandType: KClass<C>

    fun firstStep(command: C): S

    suspend fun perform(step: S): O

    fun transition(step: S, outcome: O): Transition<S>

    /** Workflows sharing a key never run concurrently, e.g. all sends of one group. */
    fun lockKey(command: C): String? = null
}
