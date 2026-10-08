package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class StartDirectIdentityResultObserverStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "direct identity result observer"

    override suspend fun runAfterIdentityReady() {
        initialization.directIdentityResultObserver.run()
    }
}
