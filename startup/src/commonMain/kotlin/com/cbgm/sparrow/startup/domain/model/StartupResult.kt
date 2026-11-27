package com.cbgm.sparrow.startup.domain.model

sealed interface StartupResult {
    data object Ready : StartupResult

    data object IdentityRequired : StartupResult

    /** The identity's keys are lost for good while chats exist; the user restores a backup or resets. */
    data object IdentityUnusable : StartupResult

    data class Error(
        val cause: Throwable
    ) : StartupResult
}
