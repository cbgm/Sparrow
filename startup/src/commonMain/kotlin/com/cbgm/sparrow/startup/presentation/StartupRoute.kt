package com.cbgm.sparrow.startup.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.identity.presentation.setup.model.IdentityUiState
import com.cbgm.sparrow.feature.onboarding.presentation.OnboardingRoute
import com.cbgm.sparrow.startup.presentation.model.StartupUiState
import com.cbgm.sparrow.startup.presentation.screen.StartupScreen
import com.cbgm.sparrow.startup.presentation.screen.StartupViewModel
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StartupRoute(
    onStartupReady: () -> Unit,
    startupViewModel: StartupViewModel = koinViewModel()
) {
    val startupUiState by startupViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(startupUiState) {
        if (startupUiState == StartupUiState.Ready) {
            delay(1000)
            startupViewModel.completeStartup()
            onStartupReady()
        }
    }

    when (val state = startupUiState) {
        StartupUiState.IdentityRequired -> {
            OnboardingRoute(
                onComplete = {
                    startupViewModel.completeStartup()
                    onStartupReady()
                }
            )
        }
        else -> {
            StartupScreen(
                uiState = state,
                identityUiState = IdentityUiState.Loading,
                onUiEvent = startupViewModel::onUiEvent
            )
        }
    }
}
