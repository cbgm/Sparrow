package com.cbgm.sparrow.feature.chats.presentation.direct

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DirectConversationRoute(
    contactId: String,
    modifier: Modifier = Modifier,
    targetMessageId: String? = null,
    viewModel: DirectConversationViewModel = koinViewModel()
) {
    val conversationState by viewModel.conversationState.collectAsStateWithLifecycle()
    val composerState by viewModel.composerState.collectAsStateWithLifecycle()
    val contextState by viewModel.contextState.collectAsStateWithLifecycle()
    val typingState by viewModel.typingState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(contactId) {
        viewModel.markConversationRead()
    }

    DisposableEffect(contactId) {
        onDispose(viewModel::stopTyping)
    }

    val incomingMessageIds =
        conversationState.messages
            .asSequence()
            .filterNot { message -> message.isMine }
            .map { message -> message.id }
            .toList()

    LaunchedEffect(incomingMessageIds) {
        if (incomingMessageIds.isNotEmpty()) {
            viewModel.markConversationRead()
        }
    }

    DirectConversationScreen(
        uiState = conversationState,
        composerState = composerState,
        contextState = contextState,
        typingState = typingState,
        errorMessage = errorMessage,
        onUiEvent = viewModel::onUiEvent,
        targetMessageId = targetMessageId,
        modifier = modifier
    )
}
