package com.cbgm.securechat.feature.contacts.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.contacts.presentation.model.BlockedContactsEffect
import com.cbgm.securechat.feature.contacts.presentation.screen.blocklist.BlockedContactsScreen
import com.cbgm.securechat.feature.contacts.presentation.screen.blocklist.BlockedContactsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BlockedContactsRoute(
    modifier: Modifier = Modifier,
    viewModel: BlockedContactsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { event ->
            when (event) {
                is BlockedContactsEffect.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        BlockedContactsScreen(
            uiState = uiState,
            onUiEvent = viewModel::onUiEvent
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
