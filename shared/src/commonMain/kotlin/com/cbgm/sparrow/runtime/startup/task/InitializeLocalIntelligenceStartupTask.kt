package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.core.util.logging.StartupTrace
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class InitializeLocalIntelligenceStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "local intelligence initialization"

    override suspend fun runAfterIdentityReady() {
        StartupTrace.measure("background local embedding") {
            initialization.initializeLocalEmbedding().getOrThrow()
        }
        StartupTrace.measure("background semantic search") {
            initialization.initializeSemanticSearch().getOrThrow()
        }
        StartupTrace.measure("background message safety") {
            initialization.initializeMessageSafety().getOrThrow()
        }
        StartupTrace.event("background local intelligence complete")
    }
}
