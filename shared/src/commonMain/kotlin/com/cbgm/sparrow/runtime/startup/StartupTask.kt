package com.cbgm.sparrow.runtime.startup

sealed interface StartupTaskResult {
    data object Completed : StartupTaskResult

    data object IdentityReady : StartupTaskResult

    data object IdentityRequired : StartupTaskResult

    data object IdentityUnusable : StartupTaskResult
}

interface StartupTask {
    val name: String
    val waitForCompletion: Boolean

    val runOnMainThread: Boolean
        get() = false

    // Marks a waiting task the transport connection depends on. The runtime starts as soon as
    // all of these have finished with a ready identity, without waiting for the remaining tasks.
    val requiredForTransport: Boolean
        get() = false

    suspend fun run(): StartupTaskResult
}
