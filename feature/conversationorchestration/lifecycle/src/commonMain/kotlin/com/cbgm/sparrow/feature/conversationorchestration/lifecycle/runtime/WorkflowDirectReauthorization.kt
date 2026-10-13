package com.cbgm.sparrow.feature.conversationorchestration.lifecycle.runtime

import com.cbgm.sparrow.feature.conversationorchestration.engine.runner.WorkflowEngine
import com.cbgm.sparrow.feature.conversationorchestration.lifecycle.domain.command.LifecycleCommand
import com.cbgm.sparrow.feature.conversationorchestration.message.domain.port.DirectReauthorizationPort

/** Lets message workflows request a fresh authorization without depending on the lifecycle module. */
internal class WorkflowDirectReauthorization(
    private val engine: WorkflowEngine
) : DirectReauthorizationPort {
    override suspend fun requestReauthorization(contactId: String): Result<Unit> =
        engine.run(LifecycleCommand.RequestReauthorization(contactId)).map { }
}
