package com.cbgm.sparrow.feature.media.presentation.filepicker

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.media.presentation.filepicker.model.FilePickerSessionResult
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FilePickerSessionController {
    private val sessions = mutableMapOf<String, FilePickerSession>()
    private val _results = MutableSharedFlow<FilePickerSessionResult>(extraBufferCapacity = 16)

    val results: SharedFlow<FilePickerSessionResult> = _results.asSharedFlow()

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

    fun snapshot(sessionId: String): FilePickerSessionSnapshot? =
        sessions[sessionId]?.let { session ->
            FilePickerSessionSnapshot(
                maxItems = session.maxItems,
                maxFileBytes = session.maxFileBytes,
                blockedSourceReferences = session.blockedSourceReferences
            )
        }

    fun complete(sessionId: String, media: List<MediaSelection>) {
        if (sessions.remove(sessionId) == null) return
        _results.tryEmit(FilePickerSessionResult.Completed(sessionId = sessionId, media = media))
    }

    fun dismiss(sessionId: String) {
        if (sessions.remove(sessionId) == null) return
        _results.tryEmit(FilePickerSessionResult.Dismissed(sessionId))
    }

    fun reportError(sessionId: String, message: String) {
        if (sessionId !in sessions) return
        _results.tryEmit(FilePickerSessionResult.Failed(sessionId = sessionId, message = message))
    }

    fun isActive(sessionId: String): Boolean = sessionId in sessions
}

data class FilePickerSessionSnapshot(
    val maxItems: Int,
    val maxFileBytes: Long,
    val blockedSourceReferences: Set<String>
)

private data class FilePickerSession(
    val maxItems: Int,
    val maxFileBytes: Long,
    val blockedSourceReferences: Set<String>
)
