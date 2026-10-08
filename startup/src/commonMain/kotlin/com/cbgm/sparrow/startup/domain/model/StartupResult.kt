package com.cbgm.sparrow.startup.domain.model

sealed interface StartupResult {
    data object Ready : StartupResult

    data object IdentityRequired : StartupResult

    data class Error(
        val cause: Throwable
    ) : StartupResult
}
