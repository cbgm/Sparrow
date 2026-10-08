package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class StartApprovedIdentityReconnectionObserverStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "approved identity reconnection observer"

    override suspend fun runAfterIdentityReady() {
        initialization.approvedIdentityReconnectionObserver.run()
    }
}
