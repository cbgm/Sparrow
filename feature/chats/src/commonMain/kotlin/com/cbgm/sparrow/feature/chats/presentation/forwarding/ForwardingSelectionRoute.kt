package com.cbgm.sparrow.feature.chats.presentation.forwarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.chats.presentation.forwarding.model.ForwardingSelectionEffect
import com.cbgm.sparrow.feature.conversationorchestration.api.command.ForwardTarget
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ForwardingSelectionRoute(
    onTargetSelected: (ForwardTarget) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForwardingSelectionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ForwardingSelectionEffect.TargetSelected ->
                    onTargetSelected(effect.target)
            }
        }
    }

    ForwardingSelectionScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent,
        onBack = onBack,
        modifier = modifier
    )
}
