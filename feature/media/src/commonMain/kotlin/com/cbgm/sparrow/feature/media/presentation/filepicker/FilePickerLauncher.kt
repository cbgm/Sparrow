package com.cbgm.sparrow.feature.media.presentation.filepicker

import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSessionResult
import kotlinx.coroutines.flow.SharedFlow

class FilePickerLauncher(
    private val sessions: FilePickerSessionController
) {
    val results: SharedFlow<FilePickerSessionResult> = sessions.results

    fun launch(
        maxItems: Int,
        maxFileBytes: Long,
        blockedSourceReferences: Set<String>
    ): String {
        require(maxItems > 0)
        require(maxFileBytes > 0)

        val sessionId =
            sessions.startSession(
                maxItems = maxItems,
                maxFileBytes = maxFileBytes,
                blockedSourceReferences = blockedSourceReferences
            )
        return sessionId
    }
}
