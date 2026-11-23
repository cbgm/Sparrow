
package com.cbgm.sparrow.navigation.routing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import com.cbgm.sparrow.core.ui.navigation.AppNavigationEvent
import com.cbgm.sparrow.core.ui.navigation.AppNavigator

@Suppress("ComposableNaming")
@Composable
fun NavHostController.bind(navigator: AppNavigator) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(navigator.navigationEvents, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navigator.navigationEvents.collect { event ->
                when (event) {
                    is AppNavigationEvent.NavigateTo -> {
                        val currentEntry = currentBackStackEntry

                        // Ignore navigation requests while the current entry is transitioning.
                        if (currentEntry?.lifecycle?.currentState != Lifecycle.State.RESUMED) {
                            return@collect
                        }

                        navigate(event.route) {
                            launchSingleTop = true

                            event.popUpTo?.let { route ->
                                popUpTo(route) {
                                    inclusive = event.inclusive
                                }
                            }
                        }
                    }

                    AppNavigationEvent.PopBackStack -> {
                        if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                            popBackStack()
                        }
                    }

                    is AppNavigationEvent.PopBackStackTo -> {
                        if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                            popBackStack(event.route, event.inclusive)
                        }
                    }
                }
            }
        }
    }
}
