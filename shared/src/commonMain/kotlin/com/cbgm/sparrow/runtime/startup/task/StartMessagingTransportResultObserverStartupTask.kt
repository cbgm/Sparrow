package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class StartMessagingTransportResultObserverStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "messaging transport result observer"

    override suspend fun runAfterIdentityReady() {
        initialization.messagingTransportResultObserver.run()
    }
}
