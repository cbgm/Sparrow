package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.feature.settings.domain.usecase.InitAppLanguageUseCase
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult

class InitializeAppLanguageStartupTask(
    private val initAppLanguageUseCase: InitAppLanguageUseCase
) : StartupTask {
    override val name = "app language"
    override val waitForCompletion = true
    override val runOnMainThread = true

    override suspend fun run(): StartupTaskResult {
        initAppLanguageUseCase()
        return StartupTaskResult.Completed
    }
}
