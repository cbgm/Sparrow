package com.cbgm.sparrow.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.locale.AppLocaleEnvironment
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.navigation.routing.AppNavigation
import org.koin.compose.viewmodel.koinViewModel
import kotlin.random.Random

/**
 * Differs in every process. Saved screen state is keyed by it, so a process that Android restarted in the
 * background does not restore the old screens past startup and the app lock.
 */
private val processKey = Random.nextLong()

@Composable
fun App(
    onReleaseNativeSplash: () -> Unit = {},
    onAppLockEnabledChanged: (Boolean) -> Unit = {},
    appViewModel: AppViewModel = koinViewModel()
) {
    ObserveAppLifecycle(appViewModel = appViewModel)

    val appLockEnabled by appViewModel.appLockEnabled.collectAsStateWithLifecycle(initialValue = false)
    LaunchedEffect(appLockEnabled) { onAppLockEnabledChanged(appLockEnabled) }

    AppLocaleEnvironment {
        SparrowTheme {
            key(processKey) {
                AppNavigation(
                    onStartupContentReady = onReleaseNativeSplash
                )
            }
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
