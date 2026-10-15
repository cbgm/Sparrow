package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class StartPendingAuthorizationCleanupStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "pending authorization cleanup"

    override suspend fun runAfterIdentityReady() {
        initialization.pendingAuthorizationCleanupRunner.run()
    }
}
