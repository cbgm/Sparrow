package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.feature.settings.domain.usecase.InitAppThemeModeUseCase
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult

class InitializeAppThemeStartupTask(
    private val initAppThemeModeUseCase: InitAppThemeModeUseCase
) : StartupTask {
    override val name = "app theme"
    override val waitForCompletion = true
    override val runOnMainThread = true

    override suspend fun run(): StartupTaskResult {
        initAppThemeModeUseCase().getOrThrow()
        return StartupTaskResult.Completed
    }
}
