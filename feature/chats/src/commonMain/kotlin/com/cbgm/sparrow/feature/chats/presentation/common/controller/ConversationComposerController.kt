package com.cbgm.sparrow.feature.chats.presentation.common.controller

import androidx.lifecycle.SavedStateHandle
import com.cbgm.sparrow.core.base.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.core.base.messagepart.domain.model.Text
import com.cbgm.sparrow.core.util.id.IdGenerator
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
    private val media: ConversationMediaController
) {
    private lateinit var state: SavedStateHandle
    private val _messageText = MutableStateFlow("")
    private val _replyToMessageId = MutableStateFlow("")
    private val _editingMessageId = MutableStateFlow("")
    private val _isSending = MutableStateFlow(false)
    private val _locationShareState = MutableStateFlow(LocationShareState.IDLE)
    val messageText: StateFlow<String> = _messageText.asStateFlow()
    val replyToMessageId: StateFlow<String> = _replyToMessageId.asStateFlow()
    val editingMessageId: StateFlow<String> = _editingMessageId.asStateFlow()
    val selectedMedia = media.selected
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()
    val locationShareState: StateFlow<LocationShareState> = _locationShareState.asStateFlow()

    internal fun bind(state: SavedStateHandle) {
        this.state = state
        _messageText.value = state[MESSAGE_TEXT_KEY] ?: ""
        _replyToMessageId.value = state[REPLY_TO_MESSAGE_ID_KEY] ?: ""
        _editingMessageId.value = state[EDITING_MESSAGE_ID_KEY] ?: ""
    }

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
        setMessageText(text)
    }

    fun reply(messageId: String) {
        setEditingMessageId("")
        setReplyToMessageId(messageId)
    }

    fun clearReply() {
        setReplyToMessageId("")
    }

    fun edit(messageId: String, text: String) {
        setReplyToMessageId("")
        media.discardSelection()
        setEditingMessageId(messageId)
        setMessageText(text)
    }

    fun cancelEdit() {
        setEditingMessageId("")
        setMessageText("")
    }

    fun clear() {
        setMessageText("")
        setReplyToMessageId("")
        setEditingMessageId("")
        media.discardSelection()
    }

    private fun setMessageText(text: String) {
        _messageText.value = text
        state[MESSAGE_TEXT_KEY] = text
    }

    private fun setReplyToMessageId(messageId: String) {
        _replyToMessageId.value = messageId
        state[REPLY_TO_MESSAGE_ID_KEY] = messageId
    }

    private fun setEditingMessageId(messageId: String) {
        _editingMessageId.value = messageId
        state[EDITING_MESSAGE_ID_KEY] = messageId
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

    private companion object {
        const val MESSAGE_TEXT_KEY = "messageText"
        const val REPLY_TO_MESSAGE_ID_KEY = "replyToMessageId"
        const val EDITING_MESSAGE_ID_KEY = "editingMessageId"
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
