package com.cbgm.sparrow.feature.media.presentation.filepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.sparrow.feature.media.device.FilePickerBackHandler
import com.cbgm.sparrow.feature.media.device.rememberFileAccessLauncher
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerUiEvent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FilePickerRoute() {
    val viewModel = koinViewModel<FilePickerViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accessLauncher =
        rememberFileAccessLauncher(
            onReturned = { reference ->
                viewModel.onUiEvent(FilePickerUiEvent.FileAccessReturned(reference))
            },
            onError = { message -> viewModel.onUiEvent(FilePickerUiEvent.FileAccessError(message)) }
        )

    FilePickerBackHandler(
        onBack = { viewModel.onUiEvent(FilePickerUiEvent.BackClicked) }
    )

    DisposableEffect(viewModel) {
        onDispose(viewModel::dismissSessionIfActive)
    }

    FilePickerScreen(
        uiState = uiState,
        onUiEvent = { event ->
            when (event) {
                FilePickerUiEvent.GrantFileAccessClicked -> accessLauncher.launch()
                else -> viewModel.onUiEvent(event)
            }
        }
    )
}
