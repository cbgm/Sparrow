package com.cbgm.securechat.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import com.cbgm.securechat.core.ui.navigation.AppNavigationEvent
import com.cbgm.securechat.core.ui.navigation.AppNavigator

@Composable
fun NavHostController.bind(navigator: AppNavigator) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(navigator.navigationEvents, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navigator.navigationEvents.collect { event ->
                when (event) {
                    is AppNavigationEvent.NavigateTo -> {
                        this@bind.navigate(event.route) {
                            launchSingleTop = true
                        }
                    }
                    is AppNavigationEvent.PopBackStack -> {
                        this@bind.popBackStack()
                    }
                    is AppNavigationEvent.PopBackStackTo -> {
                        this@bind.popBackStack(event.route, event.inclusive)
                    }
                }
            }
        }
    }
}
