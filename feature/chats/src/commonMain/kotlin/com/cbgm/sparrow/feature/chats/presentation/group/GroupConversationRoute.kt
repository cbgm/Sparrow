package com.cbgm.sparrow.feature.chats.presentation.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.logging.ChatOpenTrace
import com.cbgm.sparrow.core.ui.component.SparrowOverlayHost
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.forwarding.ForwardingSelectionRoute
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GroupConversationRoute(
    conversationId: String,
    modifier: Modifier = Modifier,
    targetMessageId: String? = null,
    viewModel: GroupConversationViewModel = koinViewModel()
) {
    val conversationState by viewModel.conversationState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        ChatOpenTrace.event("group route first composition committed")
    }
    LaunchedEffect(conversationState.isLoading, conversationState.messages.size) {
        ChatOpenTrace.event("group route state loading=${conversationState.isLoading} messages=${conversationState.messages.size}")
    }
    val composerState by viewModel.composerState.collectAsStateWithLifecycle()
    val contextState by viewModel.contextState.collectAsStateWithLifecycle()
    val indicatorState by viewModel.indicatorState.collectAsStateWithLifecycle()
    val membershipState by viewModel.membershipState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()

    val newestMessageId = conversationState.messages.firstOrNull()?.id
    LaunchedEffect(conversationId, newestMessageId) {
        viewModel.markConversationRead()
    }

    DisposableEffect(conversationId) {
        onDispose(viewModel::stopIndicator)
    }

    var forwardingMessageId by rememberSaveable { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        GroupConversationScreen(
            uiState = conversationState,
            composerState = composerState,
            contextState = contextState,
            indicatorState = indicatorState,
            membershipState = membershipState,
            historyState = historyState,
            onUiEvent = viewModel::onUiEvent,
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
                            GroupConversationUiEvent.ForwardMessage(
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
