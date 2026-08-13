package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.model.ChatUiEvent
import com.cbgm.securechat.feature.chats.presentation.screen.chat.ChatScreen
import com.cbgm.securechat.feature.chats.presentation.screen.chat.ChatViewModel
import com.cbgm.securechat.feature.chats.presentation.screen.chat.component.ManualIdentitySetupDialog
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ChatRoute(
    conversationId: String,
    contactId: String,
    contactName: String,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel =
        koinViewModel {
            parametersOf(
                conversationId,
                contactId,
                contactName
            )
        }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showManualIdentitySetupDialog by remember(contactId) { mutableStateOf(false) }

    LaunchedEffect(key1 = contactId) {
        viewModel.markConversationRead()
    }

    DisposableEffect(key1 = contactId) {
        onDispose {
            viewModel.stopTyping()
        }
    }

    val incomingMessageIds =
        uiState.messages
            .asSequence()
            .filter { message -> !message.isMine }
            .map { message -> message.id }
            .toList()

    LaunchedEffect(key1 = incomingMessageIds) {
        if (incomingMessageIds.isNotEmpty()) {
            viewModel.markConversationRead()
        }
    }

    ChatScreen(
        uiState = uiState,
        onUiEvent = { event ->
            if (event == ChatUiEvent.ManualIdentitySetupClicked) {
                showManualIdentitySetupDialog = true
            } else {
                viewModel.onUiEvent(event)
            }
        },
        modifier = modifier
    )

    if (showManualIdentitySetupDialog) {
        ManualIdentitySetupDialog(
            onShareIdentity = {
                showManualIdentitySetupDialog = false
                viewModel.onUiEvent(ChatUiEvent.ShareIdentityClicked)
            },
            onImportIdentity = {
                showManualIdentitySetupDialog = false
                viewModel.onUiEvent(ChatUiEvent.ImportIdentityClicked)
            },
            onDismiss = {
                showManualIdentitySetupDialog = false
            }
        )
    }
}
