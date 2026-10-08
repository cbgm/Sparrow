package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

class MaintainControlPlaneDirectoryStartupTask(
    private val initialization: AppInitializationDependencies
) : StartupTask {
    override val name = "control-plane directory maintenance"
    override val waitForCompletion = false

    override suspend fun run(): StartupTaskResult {
        while (currentCoroutineContext().isActive) {
            val result = initialization.controlPlaneDirectorySynchronizer.refresh()
            delay(
                (
                    if (result.isSuccess) {
                        CONTROL_PLANE_DIRECTORY_REFRESH_MILLISECONDS
                    } else {
                        CONTROL_PLANE_DIRECTORY_RETRY_MILLISECONDS
                    }
                ).milliseconds
            )
        }

        return StartupTaskResult.Completed
    }

    private companion object {
        const val CONTROL_PLANE_DIRECTORY_REFRESH_MILLISECONDS = 300_000L
        const val CONTROL_PLANE_DIRECTORY_RETRY_MILLISECONDS = 5_000L
    }
}
