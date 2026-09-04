package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GroupConversationRoute(
    conversationId: String,
    modifier: Modifier = Modifier,
    targetMessageId: String? = null,
    viewModel: GroupConversationViewModel = koinViewModel()
) {
    val conversationState by viewModel.conversationState.collectAsStateWithLifecycle()
    val composerState by viewModel.composerState.collectAsStateWithLifecycle()
    val contextState by viewModel.contextState.collectAsStateWithLifecycle()
    val typingState by viewModel.typingState.collectAsStateWithLifecycle()
    val membershipState by viewModel.membershipState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val newestMessageId = conversationState.messages.firstOrNull()?.id
    LaunchedEffect(conversationId, newestMessageId) {
        viewModel.markConversationRead()
    }

    DisposableEffect(conversationId) {
        onDispose(viewModel::stopTyping)
    }

    GroupConversationScreen(
        uiState = conversationState,
        composerState = composerState,
        contextState = contextState,
        typingState = typingState,
        membershipState = membershipState,
        errorMessage = errorMessage,
        onUiEvent = viewModel::onUiEvent,
        targetMessageId = targetMessageId,
        modifier = modifier
    )
}
