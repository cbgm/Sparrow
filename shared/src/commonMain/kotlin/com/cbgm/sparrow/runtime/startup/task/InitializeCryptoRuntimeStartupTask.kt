package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult

class InitializeCryptoRuntimeStartupTask(
    private val initialization: AppInitializationDependencies
) : StartupTask {
    override val name = "crypto runtime"
    override val waitForCompletion = true

    override suspend fun run(): StartupTaskResult {
        initialization.initializeCryptoRuntime()
            .getOrElse { error ->
                throw IllegalStateException(
                    "Sparrow could not initialize its cryptographic runtime",
                    error
                )
            }

        return StartupTaskResult.Completed
    }
}
