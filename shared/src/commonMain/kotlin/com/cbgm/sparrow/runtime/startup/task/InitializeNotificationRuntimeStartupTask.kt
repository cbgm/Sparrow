package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult

class InitializeNotificationRuntimeStartupTask(
    private val initialization: AppInitializationDependencies
) : StartupTask {
    override val name = "notification runtime"
    override val waitForCompletion = false

    override suspend fun run(): StartupTaskResult {
        initialization.platformNotificationRuntime.initialize()
        return StartupTaskResult.Completed
    }
}
