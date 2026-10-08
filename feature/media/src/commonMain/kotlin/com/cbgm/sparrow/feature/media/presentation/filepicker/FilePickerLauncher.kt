package com.cbgm.sparrow.feature.media.presentation.filepicker

import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSessionResultUi
import kotlinx.coroutines.flow.StateFlow

class FilePickerLauncher(
    private val sessions: FilePickerSessionController
) {
    private var activeSessionId: String? = null

    val results: StateFlow<Map<String, FilePickerSessionResultUi>> = sessions.results

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
        activeSessionId = sessionId
        return sessionId
    }

    fun consumeResult(): FilePickerSessionResultUi? {
        val sessionId = activeSessionId ?: return null
        val result = sessions.consumeResult(sessionId) ?: return null
        if (result is FilePickerSessionResultUi.Completed || result is FilePickerSessionResultUi.Dismissed) {
            activeSessionId = null
        }
        return result
    }
}
