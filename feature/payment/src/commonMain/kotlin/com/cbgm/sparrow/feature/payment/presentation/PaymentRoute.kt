package com.cbgm.sparrow.feature.payment.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.core.ui.component.BlockScreenshotEffect
import com.cbgm.sparrow.feature.payment.device.PaymentImageLauncher
import com.cbgm.sparrow.feature.payment.presentation.model.PaymentUiEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PaymentRoute(viewModel: PaymentViewModel = koinViewModel()) {
    BlockScreenshotEffect(enabled = true)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PaymentImageLauncher(state.imageRequest) { viewModel.onUiEvent(PaymentUiEvent.ShareCompleted(it)) }
    PaymentScreen(state, viewModel::onUiEvent)
}
