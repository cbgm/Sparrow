package com.cbgm.sparrow.feature.chats.presentation.group.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GroupRoute(
    conversationId: String,
    modifier: Modifier = Modifier,
    viewModel: GroupViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val newestMessageId = uiState.messages.firstOrNull()?.id
    LaunchedEffect(conversationId, newestMessageId) {
        viewModel.markConversationRead()
    }

    DisposableEffect(conversationId) {
        onDispose(viewModel::stopTyping)
    }

    GroupScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        modifier = modifier
    )
}
