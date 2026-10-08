package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class StartContactBlockObserverStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "contact block observer"

    override suspend fun runAfterIdentityReady() {
        initialization.contactBlockObserver.run()
    }
}
