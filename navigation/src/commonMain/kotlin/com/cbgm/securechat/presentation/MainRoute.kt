package com.cbgm.securechat.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.presentation.screen.MainScreen
import com.cbgm.securechat.presentation.screen.MainViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainRoute(
    viewModel: MainViewModel = koinViewModel()
) {
    val invitationCount by viewModel.invitationCount.collectAsStateWithLifecycle()

    MainScreen(
        invitationCount = invitationCount,
        onOpenInvitations = viewModel::openContactInvitations
    )
}
