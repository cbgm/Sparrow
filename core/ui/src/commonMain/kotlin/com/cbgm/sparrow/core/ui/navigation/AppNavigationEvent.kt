package com.cbgm.sparrow.core.ui.navigation

sealed interface AppNavigationEvent {
    data class NavigateTo(
        val route: AppRoute,
        val popUpTo: AppRoute? = null,
        val inclusive: Boolean = false
    ) : AppNavigationEvent

    data object PopBackStack : AppNavigationEvent

    data class PopBackStackTo(
        val route: AppRoute,
        val inclusive: Boolean = false
    ) : AppNavigationEvent
}
