package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.BuildKonfig
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult

class LoadControlPlaneConfigurationStartupTask(
    private val initialization: AppInitializationDependencies
) : StartupTask {
    private val logger = SparrowLog.withTag("LoadControlPlaneConfigurationStartupTask")

    override val name = "load saved control-plane configuration"
    override val waitForCompletion = true

    override suspend fun run(): StartupTaskResult {
        initialization.controlPlaneConfiguration.initialize()
        initialization.controlPlaneConfiguration
            .useDefaultDirectoryUrlIfUnconfigured(
                BuildKonfig.CONTROL_PLANE_DIRECTORY_URL.trim()
            ).onFailure { error ->
                logger.error(error) {
                    "Initial control-plane directory configuration could not be stored"
                }
            }

        return StartupTaskResult.Completed
    }
}
