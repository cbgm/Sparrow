package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult

class StartConversationNotificationCoordinatorStartupTask(
    private val initialization: AppInitializationDependencies
) : StartupTask {
    override val name = "conversation notification coordinator"
    override val waitForCompletion = false

    override suspend fun run(): StartupTaskResult {
        initialization.conversationNotificationCoordinator.start()
        return StartupTaskResult.Completed
    }
}
