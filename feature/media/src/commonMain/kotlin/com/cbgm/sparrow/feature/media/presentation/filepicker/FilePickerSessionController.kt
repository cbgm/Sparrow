package com.cbgm.sparrow.feature.media.presentation.filepicker

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSessionResultUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FilePickerSessionController {
    private val sessions = mutableMapOf<String, FilePickerSession>()
    private val _results = MutableStateFlow<Map<String, FilePickerSessionResultUi>>(emptyMap())

    val results: StateFlow<Map<String, FilePickerSessionResultUi>> = _results.asStateFlow()

    fun startSession(
        maxItems: Int,
        maxFileBytes: Long,
        blockedSourceReferences: Set<String>
    ): String {
        require(maxItems > 0)
        require(maxFileBytes > 0)

        val sessionId = IdGenerator.generate(prefix = "file-picker")
        sessions[sessionId] =
            FilePickerSession(
                maxItems = maxItems,
                maxFileBytes = maxFileBytes,
                blockedSourceReferences = blockedSourceReferences
            )
        return sessionId
    }

    fun snapshot(sessionId: String): FilePickerSessionSnapshotUi? =
        sessions[sessionId]?.let { session ->
            FilePickerSessionSnapshotUi(
                maxItems = session.maxItems,
                maxFileBytes = session.maxFileBytes,
                blockedSourceReferences = session.blockedSourceReferences
            )
        }

    fun complete(sessionId: String, media: List<MediaSelectionUi>) {
        if (sessions.remove(sessionId) == null) return
        publishResult(FilePickerSessionResultUi.Completed(sessionId = sessionId, media = media))
    }

    fun dismiss(sessionId: String) {
        if (sessions.remove(sessionId) == null) return
        publishResult(FilePickerSessionResultUi.Dismissed(sessionId))
    }

    fun reportError(sessionId: String, message: String) {
        if (sessionId !in sessions) return
        publishResult(FilePickerSessionResultUi.Failed(sessionId = sessionId, message = message))
    }

    fun consumeResult(sessionId: String): FilePickerSessionResultUi? {
        val result = _results.value[sessionId] ?: return null
        _results.value = _results.value - sessionId
        return result
    }

    fun isActive(sessionId: String): Boolean = sessionId in sessions

    private fun publishResult(result: FilePickerSessionResultUi) {
        _results.value = _results.value + (result.sessionId to result)
    }
}

data class FilePickerSessionSnapshotUi(
    val maxItems: Int,
    val maxFileBytes: Long,
    val blockedSourceReferences: Set<String>
)

private data class FilePickerSession(
    val maxItems: Int,
    val maxFileBytes: Long,
    val blockedSourceReferences: Set<String>
)
