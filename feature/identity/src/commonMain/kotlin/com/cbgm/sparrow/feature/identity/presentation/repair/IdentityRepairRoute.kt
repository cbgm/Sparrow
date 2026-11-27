package com.cbgm.sparrow.feature.identity.presentation.repair

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.identity.presentation.repair.model.IdentityRepairStageUi
import org.koin.compose.viewmodel.koinViewModel

/** Shown at startup when the identity's keys are lost for good; [onFinished] after a restore or a reset. */
@Composable
fun IdentityRepairRoute(
    onFinished: () -> Unit,
    viewModel: IdentityRepairViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.stage) {
        if (uiState.stage == IdentityRepairStageUi.FINISHED) onFinished()
    }

    IdentityRepairScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent
    )
}
