package com.cbgm.sparrow.feature.applock.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.applock.device.AppLockAuthenticationLauncher
import com.cbgm.sparrow.feature.applock.presentation.model.AppLockUiState
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_applock_prompt_reason
import com.cbgm.sparrow.resources.feature_applock_prompt_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppLockRoute(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    onContentReady: () -> Unit = {},
    viewModel: AppLockViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        AppLockUiState.Loading -> {
            Surface(modifier = modifier.fillMaxSize()) { }
        }

        AppLockUiState.Unlocked -> {
            Surface(modifier = modifier.fillMaxSize()) { }

            LaunchedEffect(Unit) {
                onUnlocked()
            }
        }

        is AppLockUiState.Locked -> {
            AppLockAuthenticationLauncher(
                requestId = state.authenticationRequestId,
                enabled = state.isAuthenticating,
                title = stringResource(Res.string.feature_applock_prompt_title),
                reason = stringResource(Res.string.feature_applock_prompt_reason),
                onResult = viewModel::onAuthenticationResult
            )

            AppLockScreen(
                isAuthenticating = state.isAuthenticating,
                onUnlockRequested = viewModel::requestAuthentication,
                modifier = modifier
            )

            LaunchedEffect(Unit) {
                onContentReady()
            }
        }
    }
}
