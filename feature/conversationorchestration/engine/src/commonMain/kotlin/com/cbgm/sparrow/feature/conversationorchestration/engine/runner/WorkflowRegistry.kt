package com.cbgm.sparrow.feature.conversationorchestration.engine.runner

import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStateMachine
import com.cbgm.sparrow.feature.conversationorchestration.engine.machine.WorkflowStep
import kotlin.reflect.KClass

/**
 * Finds the one workflow that handles a command type.
 *
 * Workflows are looked up on first use because they themselves depend on the [WorkflowEngine] that uses
 * this registry. [validate] runs the same lookup eagerly at app start, so a missing or duplicate workflow
 * fails immediately instead of when its command is first submitted.
 */
class WorkflowRegistry(
    private val provideWorkflows: () -> List<WorkflowStateMachine<*, *, *>>
) {
    private val workflowsByCommand: Map<KClass<*>, WorkflowStateMachine<*, *, *>> by lazy {
        val workflows = provideWorkflows()
        val byCommand = workflows.associateBy { workflow -> workflow.commandType }
        check(byCommand.size == workflows.size) {
            val duplicates = workflows.groupBy { it.commandType }.filterValues { it.size > 1 }.keys
            "Every command must have exactly one workflow; duplicates: ${duplicates.map { it.simpleName }}"
        }
        byCommand
    }

    fun validate() {
        workflowsByCommand
    }

    @Suppress("UNCHECKED_CAST")
    fun find(command: Any): WorkflowStateMachine<Any, WorkflowStep, Any>? =
        workflowsByCommand[command::class] as WorkflowStateMachine<Any, WorkflowStep, Any>?
}
