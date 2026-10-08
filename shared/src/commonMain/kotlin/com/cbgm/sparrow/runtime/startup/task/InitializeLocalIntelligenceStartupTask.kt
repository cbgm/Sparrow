package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.core.logging.StartupTrace
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class InitializeLocalIntelligenceStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "local intelligence initialization"

    override suspend fun runAfterIdentityReady() {
        StartupTrace.measure("background local embedding") {
            initialization.initializeLocalEmbedding()
        }
        StartupTrace.measure("background semantic search") {
            initialization.initializeSemanticSearch()
        }
        StartupTrace.measure("background message safety") {
            initialization.initializeMessageSafety()
        }
        StartupTrace.event("background local intelligence complete")
    }
}
