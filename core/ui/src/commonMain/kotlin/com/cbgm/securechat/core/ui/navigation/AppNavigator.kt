package com.cbgm.securechat.core.ui.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class AppNavigator {
    private val _navigationEvents =
        MutableSharedFlow<AppNavigationEvent>(
            replay = 0,
            extraBufferCapacity = 1
        )
    val navigationEvents: SharedFlow<AppNavigationEvent> = _navigationEvents.asSharedFlow()

    fun navigateTo(route: AppRoute) {
        _navigationEvents.tryEmit(AppNavigationEvent.NavigateTo(route))
    }

    fun navigateTo(
        route: AppRoute,
        popUpTo: AppRoute? = null,
        inclusive: Boolean = false
    ) {
        _navigationEvents.tryEmit(AppNavigationEvent.NavigateTo(route, popUpTo, inclusive))
    }

    fun popBackStack() {
        _navigationEvents.tryEmit(AppNavigationEvent.PopBackStack)
    }

    fun popBackStackTo(
        route: AppRoute,
        inclusive: Boolean = false
    ) {
        _navigationEvents.tryEmit(
            AppNavigationEvent.PopBackStackTo(
                route = route,
                inclusive = inclusive
            )
        )
    }
}
