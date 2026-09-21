package com.cbgm.sparrow.feature.chats.presentation.direct

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.component.SparrowOverlayHost
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiEvent
import com.cbgm.sparrow.feature.chats.presentation.forwarding.ForwardingSelectionRoute
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_reconnect_queued
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DirectConversationRoute(
    contactId: String,
    modifier: Modifier = Modifier,
    targetMessageId: String? = null,
    savedStateHandle: SavedStateHandle,
    onRequestReconnect: suspend (String) -> Result<Unit>,
    viewModel: DirectConversationViewModel = koinViewModel(
        parameters = { parametersOf(savedStateHandle) }
    )
) {
    val conversationState by viewModel.conversationState.collectAsStateWithLifecycle()
    val composerState by viewModel.composerState.collectAsStateWithLifecycle()
    val contextState by viewModel.contextState.collectAsStateWithLifecycle()
    val indicatorState by viewModel.indicatorState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val reconnectSuccessText = stringResource(Res.string.feature_chats_reconnect_queued)
    var reconnectBusy by remember { mutableStateOf(false) }
    var reconnectFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(contactId) {
        viewModel.markConversationRead()
    }

    DisposableEffect(contactId) {
        onDispose(viewModel::stopIndicator)
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

    var forwardingMessageId by rememberSaveable { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        DirectConversationScreen(
            uiState = conversationState,
            composerState = composerState,
            contextState = contextState,
            indicatorState = indicatorState,
            historyState = historyState,
            errorMessage = errorMessage,
            onUiEvent = viewModel::onUiEvent,
            reconnectBusy = reconnectBusy,
            reconnectFeedback = reconnectFeedback,
            onReconnectRequested = {
                if (!reconnectBusy) {
                    reconnectBusy = true
                    reconnectFeedback = null
                    scope.launch {
                        try {
                            onRequestReconnect(contactId).fold(
                                onSuccess = { reconnectFeedback = reconnectSuccessText },
                                onFailure = { reconnectFeedback = it.message ?: "Could not queue a new invitation" }
                            )
                        } catch (error: Exception) {
                            reconnectFeedback = error.message ?: "Could not queue a new invitation"
                        } finally {
                            reconnectBusy = false
                        }
                    }
                }
            },
            onForwardMessageRequested = { messageId -> forwardingMessageId = messageId },
            targetMessageId = targetMessageId,
            modifier = Modifier.fillMaxSize()
        )

        SparrowOverlayHost(
            visible = forwardingMessageId != null,
            onDismissRequest = { forwardingMessageId = null },
            horizontalPadding = MaterialTheme.spacing.zero,
            topPadding = MaterialTheme.spacing.times(6)
        ) { dismissOverlay ->
            ForwardingSelectionRoute(
                onTargetSelected = { target ->
                    forwardingMessageId?.let { messageId ->
                        dismissOverlay()
                        forwardingMessageId = null
                        viewModel.onUiEvent(
                            DirectConversationUiEvent.ForwardMessage(
                                messageId = messageId,
                                target = target
                            )
                        )
                    }
                },
                onBack = {
                    dismissOverlay()
                    forwardingMessageId = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
