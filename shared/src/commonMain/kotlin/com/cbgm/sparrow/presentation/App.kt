package com.cbgm.sparrow.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cbgm.sparrow.core.ui.locale.AppLocaleEnvironment
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.navigation.AppNavigation
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    appViewModel: AppViewModel = koinViewModel()
) {
    ObserveAppLifecycle(appViewModel = appViewModel)

    AppLocaleEnvironment {
        SparrowTheme {
            AppNavigation()
        }
    }
}

@Composable
private fun ObserveAppLifecycle(appViewModel: AppViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, appViewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> appViewModel.onAppVisible()
                    Lifecycle.Event.ON_STOP -> appViewModel.onAppHidden()
                    else -> Unit
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)

        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            appViewModel.onAppVisible()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            appViewModel.onAppHidden()
        }
    }
}
