package com.cbgm.sparrow.core.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun isDark(systemDark: Boolean): Boolean =
        when (this) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }
}

/**
 * A flow, not Compose state, for the same reason as the app language: the startup task sets it off the main composition.
 * Null until the saved choice is loaded, so the phone's remembered scheme is not replaced on the way.
 */
private val appThemeMode = MutableStateFlow<AppThemeMode?>(null)

val currentAppThemeMode: StateFlow<AppThemeMode?> = appThemeMode.asStateFlow()

fun setAppThemeMode(mode: AppThemeMode) {
    appThemeMode.value = mode
}
