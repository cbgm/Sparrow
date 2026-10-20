package com.cbgm.sparrow.feature.media.presentation.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.cbgm.sparrow.core.util.logging.SparrowLog
import com.cbgm.sparrow.feature.media.device.rememberCameraScannerLauncher
import com.cbgm.sparrow.feature.media.domain.model.CameraScanMode
import com.cbgm.sparrow.feature.media.domain.model.CameraScanResult
import com.cbgm.sparrow.feature.media.domain.usecase.PrepareMediaSelectionUseCase
import com.cbgm.sparrow.feature.media.presentation.mapper.toUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionResultUi
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

interface DocumentScanLauncher {
    fun launch()
}

@Composable
fun rememberDocumentScanLauncher(
    maxImageDimension: Int,
    maxImageBytes: Int,
    onResult: (MediaSelectionResultUi) -> Unit,
    prepareMediaSelection: PrepareMediaSelectionUseCase = koinInject()
): DocumentScanLauncher {
    val scope = rememberCoroutineScope()
    val currentResult = rememberUpdatedState(onResult)
    val scanner =
        rememberCameraScannerLauncher(
            onResult = { result ->
                val captured =
                    (result as? CameraScanResult.Document)?.media ?: return@rememberCameraScannerLauncher
                scope.launch {
                    prepareMediaSelection.fromCapturedMedia(captured).map { it.toUi() }
                        .onSuccess { selection ->
                            currentResult.value(MediaSelectionResultUi.Selected(listOf(selection)))
                        }.onFailure { error ->
                            SparrowLog.error(TAG, STORE_FAILED, error)
                            currentResult.value(MediaSelectionResultUi.Error(error.message ?: STORE_FAILED))
                        }
                }
            },
            onDismissed = { currentResult.value(MediaSelectionResultUi.Dismissed) },
            onError = { message -> currentResult.value(MediaSelectionResultUi.Error(message)) }
        )
    val mode =
        remember(maxImageDimension, maxImageBytes) {
            CameraScanMode.Document(maxImageDimension = maxImageDimension, maxImageBytes = maxImageBytes)
        }

    return remember(scanner, mode) {
        object : DocumentScanLauncher {
            override fun launch() {
                scanner.launch(mode)
            }
        }
    }
}

private const val TAG = "DocumentScanLauncher"
private const val STORE_FAILED = "Scanned document could not be stored"
