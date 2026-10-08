package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult
import kotlinx.coroutines.flow.first

abstract class IdentityReadyStartupTask(
    protected val initialization: AppInitializationDependencies
) : StartupTask {
    final override val waitForCompletion = false

    final override suspend fun run(): StartupTaskResult {
        initialization.observeLocalIdentityReady().first { ready -> ready }
        runAfterIdentityReady()
        return StartupTaskResult.Completed
    }

    protected abstract suspend fun runAfterIdentityReady()
}
