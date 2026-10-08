package com.cbgm.sparrow.feature.chats.presentation.common.controller

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.media.domain.repository.MediaSelectionFileRepository
import com.cbgm.sparrow.feature.media.presentation.model.FileMediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.MediaTypeUi
import com.cbgm.sparrow.feature.media.presentation.model.VisualMediaSelectionUi
import com.cbgm.sparrow.feature.media.presentation.model.localFilePaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationMediaController(
    private val mediaFiles: MediaSelectionFileRepository
) {
    private val _selected = MutableStateFlow<List<MediaSelectionUi>>(emptyList())
    val selected: StateFlow<List<MediaSelectionUi>> = _selected.asStateFlow()

    // Cleanup must outlive the ViewModel scope when its composer is discarded.
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val logger = SparrowLog.withTag("ConversationMediaController")

    fun select(media: List<MediaSelectionUi>): Result<Unit> = runCatching {
        validate(media)
    }.onSuccess {
        val removed = selected.value.filterNot { old -> media.any { it.id == old.id } }
        _selected.value = media
        deleteLocalCopies(removed)
    }.onFailure {
        val accepted = selected.value.mapTo(mutableSetOf(), MediaSelectionUi::id)
        deleteLocalCopies(media.filterNot { it.id in accepted })
    }

    fun discardSelection() {
        val old = selected.value
        _selected.value = emptyList()
        deleteLocalCopies(old)
    }

    fun deleteLocalCopies(media: List<MediaSelectionUi>) {
        if (media.isEmpty()) return
        cleanupScope.launch {
            for (item in media) {
                for (path in item.localFilePaths) {
                    runCatching { mediaFiles.delete(path) }
                        .onFailure { logger.error(it) { "Could not clean up pending media" } }
                }
            }
        }
    }

    fun deleteLocalPaths(paths: List<String>) {
        if (paths.isEmpty()) return
        cleanupScope.launch {
            for (path in paths.distinct()) {
                runCatching { mediaFiles.delete(path) }
                    .onFailure { logger.error(it) { "Could not clean up attachment media" } }
            }
        }
    }

    private fun validate(media: List<MediaSelectionUi>) {
        require(media.size <= MessageAttachmentPolicy.MAX_ATTACHMENTS_PER_MESSAGE) { "Too many attachments selected" }
        require(
            media.map(MediaSelectionUi::id).distinct().size == media.size
        ) { "Attachment IDs must be unique" }
        require(media.sumOf(MediaSelectionUi::byteSize) <= MessageAttachmentPolicy.MAX_TOTAL_ATTACHMENT_BYTES) {
            "Selected attachments exceed the total attachment size limit"
        }
        for (item in media) {
            require(item.byteSize > 0L) { "Selected attachment is empty" }
            when (item) {
                is VisualMediaSelectionUi -> when (item.type) {
                    MediaTypeUi.IMAGE -> {
                        require(item.byteSize <= MessageAttachmentPolicy.MAX_IMAGE_BYTES) { "Image attachment too large" }
                        require(item.mimeType.startsWith("image/") && item.width != null && item.height != null) {
                            "Invalid image attachment"
                        }
                    }

                    MediaTypeUi.VIDEO -> require(
                        item.byteSize <= MessageAttachmentPolicy.MAX_VIDEO_BYTES && item.mimeType.startsWith(
                            "video/"
                        )
                    ) { "Invalid video attachment" }
                }

                is FileMediaSelectionUi -> require(
                    item.byteSize <= MessageAttachmentPolicy.MAX_FILE_BYTES && item.fileName.isNotBlank()
                ) { "Invalid file attachment" }
            }
        }
    }
}
