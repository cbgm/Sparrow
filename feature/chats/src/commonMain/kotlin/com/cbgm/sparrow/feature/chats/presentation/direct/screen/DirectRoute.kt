package com.cbgm.sparrow.feature.chats.presentation.direct.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DirectRoute(
    contactId: String,
    modifier: Modifier = Modifier,
    viewModel: DirectViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(contactId) {
        viewModel.markConversationRead()
    }

    DisposableEffect(contactId) {
        onDispose(viewModel::stopTyping)
    }

    val incomingMessageIds =
        uiState.messages
            .asSequence()
            .filterNot { message -> message.isMine }
            .map { message -> message.id }
            .toList()

    LaunchedEffect(incomingMessageIds) {
        if (incomingMessageIds.isNotEmpty()) {
            viewModel.markConversationRead()
        }
    }

    DirectScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        modifier = modifier
    )
}
