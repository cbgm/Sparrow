package com.cbgm.sparrow.navigation.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainRoute(
    viewModel: MainViewModel = koinViewModel()
) {
    val invitationCount by viewModel.invitationCount.collectAsStateWithLifecycle()
    val isMessageSearchAvailable by viewModel.isMessageSearchAvailable.collectAsStateWithLifecycle()

    MainScreen(
        invitationCount = invitationCount,
        isMessageSearchAvailable = isMessageSearchAvailable,
        onOpenSearch = viewModel::openMessageSearch,
        onOpenInvitations = viewModel::openContactInvitations
    )
}
