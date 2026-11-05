package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.feature.identity.domain.model.IdentityResolution
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult

class ResolveIdentityStatusStartupTask(
    private val initialization: AppInitializationDependencies
) : StartupTask {
    override val name = "resolve local identity status"
    override val requiredForTransport = true
    override val waitForCompletion = true

    override suspend fun run(): StartupTaskResult =
        when (initialization.resolveIdentityStatus().getOrThrow()) {
            IdentityResolution.READY -> StartupTaskResult.IdentityReady
            IdentityResolution.NOT_CREATED -> StartupTaskResult.IdentityRequired
            IdentityResolution.KEYS_UNUSABLE -> StartupTaskResult.IdentityUnusable
        }
}
