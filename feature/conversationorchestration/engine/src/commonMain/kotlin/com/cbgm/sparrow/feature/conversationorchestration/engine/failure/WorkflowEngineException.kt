package com.cbgm.sparrow.feature.conversationorchestration.engine.failure

/**
 * A broken invariant of the engine itself (a transition cycle, a running state taken as final), not a state that
 * forbids the command, so it is published as UNEXPECTED rather than NOT_ALLOWED.
 */
class WorkflowEngineException(
    message: String
) : IllegalStateException(message)
