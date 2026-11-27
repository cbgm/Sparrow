package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class PublishUnpublishedGroupStateStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "publish unpublished group state"

    override suspend fun runAfterIdentityReady() {
        initialization.unpublishedGroupStateRunner.run().getOrThrow()
    }
}
