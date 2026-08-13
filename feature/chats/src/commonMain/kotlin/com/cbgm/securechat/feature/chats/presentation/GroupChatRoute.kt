package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.screen.chat.ChatScreen
import com.cbgm.securechat.feature.chats.presentation.screen.chat.GroupChatViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.details.GroupVerificationViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupChatRoute(
    conversationId: String,
    modifier: Modifier = Modifier
) {
    val viewModel = koinViewModel<GroupChatViewModel> { parametersOf(conversationId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val verificationViewModel =
        koinViewModel<GroupVerificationViewModel> {
            parametersOf(conversationId)
        }
    val verificationUiState by verificationViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(conversationId) {
        viewModel.markConversationRead()
    }

    DisposableEffect(conversationId) {
        onDispose {
            viewModel.stopTyping()
        }
    }

    val verificationSummary = verificationUiState.summary
    val hasVerificationSnapshot = verificationSummary.hasAuthoritativeState

    ChatScreen(
        uiState =
            uiState.copy(
                groupMemberCount =
                    if (hasVerificationSnapshot) {
                        verificationSummary.totalMemberCount
                    } else {
                        uiState.groupMemberCount
                    },
                groupReadyMemberCount =
                    if (hasVerificationSnapshot) {
                        verificationSummary.activeParticipantCount
                    } else {
                        uiState.groupReadyMemberCount
                    },
                errorMessage = verificationUiState.errorMessage ?: uiState.errorMessage
            ),
        onUiEvent = viewModel::onUiEvent,
        modifier = modifier
    )
}
