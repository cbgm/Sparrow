package com.cbgm.sparrow.feature.media.presentation.filepicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.cbgm.sparrow.core.ui.navigation.AppNavigator
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.feature.media.presentation.model.FileSelection
import org.koin.compose.koinInject

interface FilePickerLauncher {
    fun launch()
}

@Composable
fun rememberFilePickerLauncher(
    maxItems: Int,
    maxFileBytes: Long,
    blockedSourceReferences: Set<String>,
    onFilesSelected: (List<FileSelection>) -> Unit,
    onDismissed: () -> Unit,
    onError: (String) -> Unit,
    navigator: AppNavigator = koinInject(),
    sessions: FilePickerSessionController = koinInject()
): FilePickerLauncher {
    val currentMaxItems = rememberUpdatedState(maxItems)
    val currentMaxFileBytes = rememberUpdatedState(maxFileBytes)
    val currentBlockedReferences = rememberUpdatedState(blockedSourceReferences)
    val currentOnFilesSelected = rememberUpdatedState(onFilesSelected)
    val currentOnDismissed = rememberUpdatedState(onDismissed)
    val currentOnError = rememberUpdatedState(onError)

    return remember(navigator, sessions) {
        object : FilePickerLauncher {
            override fun launch() {
                if (currentMaxItems.value <= 0) {
                    currentOnError.value("No more files can be selected")
                    return
                }

                val sessionId =
                    sessions.startSession(
                        maxItems = currentMaxItems.value,
                        maxFileBytes = currentMaxFileBytes.value,
                        blockedSourceReferences = currentBlockedReferences.value,
                        onFilesSelected = { files -> currentOnFilesSelected.value(files) },
                        onDismissed = { currentOnDismissed.value() },
                        onError = { message -> currentOnError.value(message) }
                    )
                navigator.navigateTo(AppRoute.FilePicker(sessionId))
            }
        }
    }
}
