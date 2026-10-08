package com.cbgm.sparrow.feature.chats.presentation.direct

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.domain.model.ForwardingTarget
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareEvent
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectComposerState
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessageDispatchResult
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DeleteDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.EditDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.MarkDirectConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.RetryDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SendOrQueueDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ToggleDirectMessageReactionUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.forward.ForwardMessageUseCase
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationComposerController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationErrors
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationMediaController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationVoiceController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.SharedContactController
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiState
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DirectConversationActionsController(
    private val markRead: MarkDirectConversationReadUseCase,
    private val composerController: ConversationComposerController,
    private val mediaController: ConversationMediaController,
    private val voiceController: ConversationVoiceController,
    private val sharedContactController: SharedContactController,
    private val forwardMessageUseCase: ForwardMessageUseCase,
    private val sendOrQueueDirectMessage: SendOrQueueDirectMessageUseCase,
    private val retryMessage: RetryDirectMessageUseCase,
    private val toggleMessageReaction: ToggleDirectMessageReactionUseCase,
    private val deleteMessageUseCase: DeleteDirectMessageUseCase,
    private val editMessageUseCase: EditDirectMessageUseCase
) {
    private lateinit var scope: CoroutineScope
    private lateinit var conversationState: StateFlow<DirectConversationUiState>
    private lateinit var composerState: StateFlow<MessageComposerUiState>
    private lateinit var indicatorController: IndicatorController
    private lateinit var errors: ConversationErrors
    private lateinit var lookupForwardMessage: suspend (String) -> com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessage?
    private lateinit var conversationId: String
    private lateinit var contactId: String
    private val logger = SparrowLog.withTag("DirectConversationActionsController")

    private val messageText get() = composerController.messageText
    private val editingMessageId get() = composerController.editingMessageId
    private val replyToMessageId get() = composerController.replyToMessageId
    private val isSending get() = composerController.isSending

    internal fun bind(
        conversationId: String,
        contactId: String,
        scope: CoroutineScope,
        conversationState: StateFlow<DirectConversationUiState>,
        composerState: StateFlow<MessageComposerUiState>,
        indicatorController: IndicatorController,
        errors: ConversationErrors,
        lookupForwardMessage: suspend (String) -> com.cbgm.sparrow.feature.chats.domain.model.direct.DirectMessage?
    ) {
        this.conversationId = conversationId
        this.contactId = contactId
        this.scope = scope
        this.conversationState = conversationState
        this.composerState = composerState
        this.indicatorController = indicatorController
        this.errors = errors
        this.lookupForwardMessage = lookupForwardMessage
    }

    fun markConversationRead() {
        scope.launch { markRead(conversationId).onFailure { logger.error(it) { "Could not mark direct conversation as read" } } }
    }

    fun forwardMessage(
        messageId: String,
        target: ForwardingTarget
    ) {
        scope.launch {
            val message =
                lookupForwardMessage(messageId)
                    ?: return@launch

            forwardMessageUseCase(
                parts = message.parts,
                target = target
            ).onFailure { error ->
                errors.report(error.message ?: "Message could not be forwarded")
            }
        }
    }

    fun onMessageTextChanged(value: String) {
        val directComposerState = conversationState.value.composerState
        if (!composerState.value.availability.isInputEnabled) return

        composerController.textChanged(value)
        errors.clear()
        indicatorController.onLocalTextChanged(
            value,
            sendsIndicators = directComposerState.sendsIndicators
        )
    }

    fun sendCurrentMessage() {
        val editId = editingMessageId.value.takeIf(String::isNotBlank)
        if (editId != null) {
            val text = messageText.value.trim()
            if (text.isNotEmpty()) editCurrentMessage(editId, text)
            return
        }
        composerController.prepareParts()
            .onSuccess { parts ->
                if (parts.isNotEmpty()) {
                    dispatchSend(
                        parts = parts,
                        clearComposerOnSuccess = true
                    )
                }
            }
            .onFailure { errors.report(it.message ?: "Selected media could not be prepared") }
    }

    fun sendVoiceMessage() {
        scope.launch {
            voiceController.recordedPart()
                .onSuccess { part ->
                    dispatchSend(
                        parts = listOf(part),
                        clearComposerOnSuccess = false,
                        clearVoiceOnSuccess = true
                    )
                }.onFailure { error ->
                    errors.report(error.message ?: "Voice message is not ready to send")
                }
        }
    }

    fun shareCurrentLocation(part: MessagePart) {
        transitionLocationShare(LocationShareEvent.LOCATION_CAPTURED)
        sendAttachmentOnly(part, isLocationShare = true)
    }

    fun sendAttachmentOnly(
        part: MessagePart,
        isLocationShare: Boolean = false
    ) {
        dispatchSend(
            parts = listOf(part),
            clearComposerOnSuccess = false,
            isLocationShare = isLocationShare
        )
    }

    private fun dispatchSend(
        parts: List<MessagePart>,
        clearComposerOnSuccess: Boolean,
        clearVoiceOnSuccess: Boolean = false,
        isLocationShare: Boolean = false
    ) {
        val directComposerState = conversationState.value.composerState
        val sendAllowed =
            if (isLocationShare) {
                !conversationState.value.isLoading && directComposerState.isSendActionEnabled && !isSending.value
            } else {
                composerState.value.availability.isSendEnabled
            }

        if (!sendAllowed) {
            if (isLocationShare) transitionLocationShare(LocationShareEvent.FAILED)
            return
        }

        val replyTo = replyToMessageId.value.takeIf(String::isNotBlank)
        errors.clear()
        scope.launch {
            if (isLocationShare) transitionLocationShare(LocationShareEvent.SEND_STARTED)
            composerController.setSending(true)
            try {
                sendOrQueueDirectMessage(
                    contactId = contactId,
                    conversationId = conversationId,
                    parts = parts,
                    replyToMessageId = replyTo
                ).onSuccess { result ->
                    when {
                        clearComposerOnSuccess -> clearComposer()
                        clearVoiceOnSuccess -> {
                            voiceController.finishSent(parts)
                            clearReply()
                        }

                        else -> clearReply()
                    }
                    if (result is DirectMessageDispatchResult.QueuedWithIdentityExchangeFailure) {
                        errors.report(
                            result.throwable.message
                                ?: "Conversation authorization could not be started"
                        )
                    }
                }.onFailure { error ->
                    val fallbackMessage =
                        if (directComposerState == DirectComposerState.READY) {
                            "Message could not be sent"
                        } else {
                            "Message could not be queued"
                        }
                    errors.report(error.message ?: fallbackMessage)
                }
            } finally {
                composerController.setSending(false)
                if (isLocationShare) transitionLocationShare(LocationShareEvent.COMPLETED)
            }
        }
    }

    fun addSharedContact(contact: SharedContact) {
        scope.launch {
            sharedContactController.add(contact)
                .onSuccess { errors.clear() }
                .onFailure { errors.report(it.message ?: "Contact could not be added") }
        }
    }

    fun updateMediaSelection(media: List<MediaSelectionUi>) {
        mediaController.select(media)
            .onSuccess { errors.clear() }
            .onFailure { errors.report(it.message ?: "Selected attachments could not be attached") }
    }

    private fun clearComposer() {
        composerController.clear()
        indicatorController.stopLocalIndicator()
    }

    fun startReply(messageId: String) {
        if (conversationState.value.messages.none { it.id == messageId }) return
        composerController.reply(messageId)
        errors.clear()
    }

    fun clearReply() = composerController.clearReply()

    fun startEdit(messageId: String) {
        val message = conversationState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (!message.canEdit) return
        val text = message.textPart?.text?.takeIf(String::isNotBlank) ?: return
        composerController.edit(messageId, text)
        errors.clear()
    }

    fun cancelEdit() {
        composerController.cancelEdit()
        errors.clear()
    }

    private fun editCurrentMessage(messageId: String, text: String) {
        if (isSending.value) return
        scope.launch {
            errors.clear()
            composerController.setSending(true)
            try {
                editMessageUseCase(conversationId, messageId, text)
                    .onSuccess { clearComposer() }
                    .onFailure { errors.report(it.message ?: "Message could not be edited") }
            } finally {
                composerController.setSending(false)
            }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) =
        executeAction("Reaction could not be sent") {
            toggleMessageReaction(conversationId, messageId, emoji)
        }

    fun deleteMessage(messageId: String) = executeAction("Message could not be deleted") {
        deleteMessageUseCase(conversationId, messageId)
    }

    fun retryFailedMessage(messageId: String) = executeAction("Message could not be queued again") {
        retryMessage(messageId)
    }

    private fun executeAction(message: String, action: suspend () -> Result<*>) {
        scope.launch {
            action().onFailure { errors.report(it.message ?: message) }
        }
    }

    fun transitionLocationShare(event: LocationShareEvent) =
        composerController.transitionLocation(event)
}
