package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

class MaintainControlPlaneHealthStartupTask(
    private val initialization: AppInitializationDependencies
) : StartupTask {
    override val name = "control-plane health maintenance"
    override val waitForCompletion = false

    override suspend fun run(): StartupTaskResult {
        while (currentCoroutineContext().isActive) {
            initialization.controlPlaneHealthMonitor.refresh()
            delay(CONTROL_PLANE_HEALTH_REFRESH_MILLISECONDS.milliseconds)
        }

        return StartupTaskResult.Completed
    }

    private companion object {
        const val CONTROL_PLANE_HEALTH_REFRESH_MILLISECONDS = 60_000L
    }
}
