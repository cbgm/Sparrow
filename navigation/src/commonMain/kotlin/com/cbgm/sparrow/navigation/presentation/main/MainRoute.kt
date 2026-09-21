package com.cbgm.sparrow.navigation.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainRoute(
    onMainReady: () -> Unit = {},
    viewModel: MainViewModel = koinViewModel()
) {
    val invitationCount by viewModel.invitationCount.collectAsStateWithLifecycle()
    val isMessageSearchAvailable by viewModel.isMessageSearchAvailable.collectAsStateWithLifecycle()

    // An effect runs after Main has been composed, independent of Room data or
    // onGloballyPositioned (which is not a safe gate for the native splash).
    LaunchedEffect(Unit) { onMainReady() }

    MainScreen(
        invitationCount = invitationCount,
        isMessageSearchAvailable = isMessageSearchAvailable,
        onOpenSearch = viewModel::openMessageSearch,
        onOpenInvitations = viewModel::openInvitations
    )
}
