package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.logging.StartupTrace
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult

class RestoreControlPlaneDirectoryStartupTask(
    private val initialization: AppInitializationDependencies
) : StartupTask {
    private val logger = SparrowLog.withTag("RestoreControlPlaneDirectoryStartupTask")

    override val name = "restore cached control-plane directory"
    override val waitForCompletion = false

    override suspend fun run(): StartupTaskResult {
        initialization.controlPlaneDirectorySynchronizer.restoreCached()
            .onFailure { error ->
                logger.warn {
                    "Verified Control Plane cache could not be restored: ${error.message}"
                }
            }

        StartupTrace.event(
            "saved control-plane endpoints=" +
                "${initialization.controlPlaneConfiguration.endpoints.value.size}; " +
                "signed-directory-configured=" +
                "${initialization.controlPlaneConfiguration.directoryUrl.value != null}"
        )

        return StartupTaskResult.Completed
    }
}
