package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.feature.identity.domain.model.IdentityStatus
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies
import com.cbgm.sparrow.runtime.startup.StartupTask
import com.cbgm.sparrow.runtime.startup.StartupTaskResult

class ResolveIdentityStatusStartupTask(
    private val initialization: AppInitializationDependencies
) : StartupTask {
    override val name = "resolve local identity status"
    override val waitForCompletion = true

    override suspend fun run(): StartupTaskResult {
        val initialStatus = initialization.getIdentityStatus().getOrThrow()
        val resolvedStatus =
            if (initialStatus == IdentityStatus.INCOMPLETE) {
                initialization.recoverIncompleteIdentity().getOrThrow()
                initialization.getIdentityStatus().getOrThrow()
            } else {
                initialStatus
            }

        return when (resolvedStatus) {
            IdentityStatus.READY -> StartupTaskResult.IdentityReady
            IdentityStatus.NOT_CREATED,
            IdentityStatus.INCOMPLETE -> StartupTaskResult.IdentityRequired
        }
    }
}
