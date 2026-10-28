package com.cbgm.sparrow.runtime.startup.task

import com.cbgm.sparrow.core.util.time.SystemClock
import com.cbgm.sparrow.presentation.model.AppInitializationDependencies

class RecoverInterruptedDirectSendsStartupTask(
    initialization: AppInitializationDependencies
) : IdentityReadyStartupTask(initialization) {
    override val name = "recover interrupted direct sends"

    /** Only sends from before this start can be interrupted; newer ones may still be on their way to the outbox. */
    private val appStartedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()

    override suspend fun runAfterIdentityReady() {
        initialization.recoverInterruptedDirectSends(appStartedAtEpochMilliseconds).getOrThrow()
    }
}
