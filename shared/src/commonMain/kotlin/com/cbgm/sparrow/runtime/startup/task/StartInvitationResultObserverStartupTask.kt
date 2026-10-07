package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class StartInvitationResultObserverStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "invitation result observer"

    override suspend fun runAfterIdentityReady() {
        initialization.invitationResultObserver.run()
    }
}
