package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class StartMembershipResultObserverStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "membership result observer"

    override suspend fun runAfterIdentityReady() {
        initialization.membershipResultObserver.run()
    }
}
