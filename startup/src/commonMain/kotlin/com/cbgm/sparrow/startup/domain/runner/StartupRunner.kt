package com.cbgm.sparrow.startup.domain.runner

import com.cbgm.sparrow.startup.domain.model.StartupResult

interface StartupRunner {
    suspend fun run(): StartupResult

    fun startPostNavigationRuntime()
}
