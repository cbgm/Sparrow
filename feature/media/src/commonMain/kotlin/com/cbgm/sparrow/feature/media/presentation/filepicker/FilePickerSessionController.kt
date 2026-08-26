package com.cbgm.sparrow.feature.media.presentation.filepicker

import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.feature.media.presentation.model.AttachmentSelection

class FilePickerSessionController {
    private val sessions = mutableMapOf<String, FilePickerSession>()

    fun startSession(
        maxItems: Int,
        maxFileBytes: Long,
        blockedSourceReferences: Set<String>,
        onFilesSelected: (List<AttachmentSelection>) -> Unit,
        onDismissed: () -> Unit,
        onError: (String) -> Unit
    ): String {
        require(maxItems > 0)
        require(maxFileBytes > 0)

        val sessionId = IdGenerator.generate(prefix = "file-picker")
        sessions[sessionId] =
            FilePickerSession(
                maxItems = maxItems,
                maxFileBytes = maxFileBytes,
                blockedSourceReferences = blockedSourceReferences,
                onFilesSelected = onFilesSelected,
                onDismissed = onDismissed,
                onError = onError
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

    fun complete(sessionId: String, files: List<AttachmentSelection>) {
        val session = sessions.remove(sessionId) ?: return
        session.onFilesSelected(files)
    }

    fun dismiss(sessionId: String) {
        sessions.remove(sessionId)?.onDismissed?.invoke()
    }

    fun reportError(sessionId: String, message: String) {
        sessions[sessionId]?.onError?.invoke(message)
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
    val blockedSourceReferences: Set<String>,
    val onFilesSelected: (List<AttachmentSelection>) -> Unit,
    val onDismissed: () -> Unit,
    val onError: (String) -> Unit
)
