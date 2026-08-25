package com.cbgm.sparrow.feature.contactimport.presentation.importing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.contactimport.presentation.component.ScannedIdentityConfirmationDialog
import com.cbgm.sparrow.feature.contactimport.presentation.importing.model.ImportIdentityUiEvent
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_contactimport_trust_and_import
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ImportIdentityRoute(
    viewModel: ImportIdentityViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ImportIdentityScreen(
        uiState = uiState,
        onUiEvent = viewModel::onUiEvent
    )

    uiState.scannedIdentityPreview?.let { preview ->
        ScannedIdentityConfirmationDialog(
            preview = preview,
            confirmButtonText = stringResource(Res.string.feature_contactimport_trust_and_import),
            onConfirm = {
                viewModel.onUiEvent(ImportIdentityUiEvent.ScannedIdentityConfirmed)
            },
            onDismiss = {
                viewModel.onUiEvent(ImportIdentityUiEvent.ScannedIdentityDismissed)
            }
        )
    }
}
