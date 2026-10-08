package com.cbgm.sparrow.feature.chats.presentation.common.controller

import androidx.lifecycle.SavedStateHandle
import com.cbgm.sparrow.core.id.IdGenerator
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.messagepart.domain.model.Text
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toMessagePart
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareEvent
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareState
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareStateMachine
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class ConversationComposerController(
    state: SavedStateHandle,
    private val media: ConversationMediaController
) {
    private val _messageText = state.getMutableStateFlow("messageText", "")
    private val _replyToMessageId = state.getMutableStateFlow("replyToMessageId", "")
    private val _editingMessageId = state.getMutableStateFlow("editingMessageId", "")
    private val _isSending = MutableStateFlow(false)
    private val _locationShareState = MutableStateFlow(LocationShareState.IDLE)
    val messageText: StateFlow<String> = _messageText.asStateFlow()
    val replyToMessageId: StateFlow<String> = _replyToMessageId.asStateFlow()
    val editingMessageId: StateFlow<String> = _editingMessageId.asStateFlow()
    val selectedMedia = media.selected
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    val locationShareState: StateFlow<LocationShareState> = _locationShareState.asStateFlow()

    fun setSending(sending: Boolean) {
        _isSending.value = sending
    }

    val draft = combine(_messageText, _replyToMessageId, _editingMessageId) { text, reply, edit ->
        ComposerDraft(text, reply.takeIf(String::isNotBlank), edit.takeIf(String::isNotBlank))
    }
    val runtime =
        combine(selectedMedia, _isSending, _locationShareState) { selected, sending, location ->
            ComposerRuntime(selected, sending, location)
        }

    fun textChanged(text: String) {
        _messageText.value = text
    }

    fun reply(messageId: String) {
        _editingMessageId.value = ""
        _replyToMessageId.value = messageId
    }

    fun clearReply() {
        _replyToMessageId.value = ""
    }

    fun edit(messageId: String, text: String) {
        _replyToMessageId.value = ""
        media.discardSelection()
        _editingMessageId.value = messageId
        _messageText.value = text
    }

    fun cancelEdit() {
        _editingMessageId.value = ""
        _messageText.value = ""
    }

    fun clear() {
        _messageText.value = ""
        _replyToMessageId.value = ""
        _editingMessageId.value = ""
        media.discardSelection()
    }

    fun transitionLocation(event: LocationShareEvent) {
        _locationShareState.value =
            LocationShareStateMachine.transition(locationShareState.value, event)
    }

    /** Prepare parts synchronously; the caller sends via existing group/direct message use cases. */
    fun prepareParts(): Result<List<MessagePart>> = runCatching {
        val text = messageText.value.trim()
        val selected = selectedMedia.value
        if (text.isEmpty() && selected.isEmpty()) return@runCatching emptyList()
        buildList {
            if (text.isNotEmpty()) {
                add(
                    Text(
                        id = IdGenerator.generate(prefix = "text"),
                        text = text
                    )
                )
            }
            addAll(selected.map { it.toMessagePart() })
        }
    }
}

data class ComposerDraft(
    val text: String,
    val replyToMessageId: String?,
    val editingMessageId: String?
)

data class ComposerRuntime(
    val media: List<MediaSelectionUi>,
    val isSending: Boolean,
    val locationShareState: LocationShareState
)
