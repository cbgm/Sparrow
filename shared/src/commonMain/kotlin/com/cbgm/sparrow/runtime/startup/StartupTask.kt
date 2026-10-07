package com.cbgm.sparrow.runtime.startup

sealed interface StartupTaskResult {
    data object Completed : StartupTaskResult

    data object IdentityReady : StartupTaskResult

    data object IdentityRequired : StartupTaskResult
}

interface StartupTask {
    val name: String
    val waitForCompletion: Boolean

    val runOnMainThread: Boolean
        get() = false

    suspend fun run(): StartupTaskResult
}
