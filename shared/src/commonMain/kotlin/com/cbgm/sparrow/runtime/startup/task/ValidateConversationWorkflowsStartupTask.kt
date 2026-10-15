package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult

class ValidateConversationWorkflowsStartupTask(
    private val initialization: AppInitializationDependencies
) : StartupTask {
    override val name = "conversation workflows"
    override val waitForCompletion = false

    override suspend fun run(): StartupTaskResult {
        initialization.workflowRegistry.validate()
        return StartupTaskResult.Completed
    }
}
