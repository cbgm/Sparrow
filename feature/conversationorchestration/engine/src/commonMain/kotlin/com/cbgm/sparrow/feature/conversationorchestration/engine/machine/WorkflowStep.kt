package com.cbgm.sparrow.feature.conversationorchestration.engine.machine

/** One state of a workflow state machine. Its [name] is published as the running step. */
interface WorkflowStep {
    val name: String
        get() = this::class.simpleName ?: "Step"
}
