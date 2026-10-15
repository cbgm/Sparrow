package com.cbgm.sparrow.feature.chats.presentation.group

import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePart
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareEvent
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupMessage
import com.cbgm.sparrow.feature.chats.presentation.common.composer.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationComposerController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationErrors
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationMediaController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.ConversationVoiceController
import com.cbgm.sparrow.feature.chats.presentation.common.controller.SharedContactController
import com.cbgm.sparrow.feature.chats.presentation.common.history.mapper.textOrNull
import com.cbgm.sparrow.feature.chats.presentation.group.model.GroupConversationUiState
import com.cbgm.sparrow.feature.conversationorchestration.api.ConversationOrchestrator
import com.cbgm.sparrow.feature.conversationorchestration.api.awaitResult
import com.cbgm.sparrow.feature.conversationorchestration.api.command.ForwardMessage
import com.cbgm.sparrow.feature.conversationorchestration.api.command.ForwardTarget
import com.cbgm.sparrow.feature.conversationorchestration.api.command.GroupMessageCommand
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelectionUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GroupConversationActionsController(
    private val orchestrator: ConversationOrchestrator,
    private val composerController: ConversationComposerController,
    private val mediaController: ConversationMediaController,
    private val voiceController: ConversationVoiceController,
    private val sharedContactController: SharedContactController
) {
    private lateinit var scope: CoroutineScope
    private lateinit var conversationState: StateFlow<GroupConversationUiState>
    private lateinit var composerState: StateFlow<MessageComposerUiState>
    private lateinit var indicatorController: GroupIndicatorController
    private lateinit var errors: ConversationErrors
    private lateinit var lookupForwardMessage: suspend (String) -> GroupMessage?
    private lateinit var groupId: String
    private val logger = SparrowLog.withTag("GroupConversationActionsController")

    private val messageText get() = composerController.messageText
    private val editingMessageId get() = composerController.editingMessageId
    private val replyToMessageId get() = composerController.replyToMessageId
    private val isSending get() = composerController.isSending

    internal fun bind(
        groupId: String,
        scope: CoroutineScope,
        conversationState: StateFlow<GroupConversationUiState>,
        composerState: StateFlow<MessageComposerUiState>,
        indicatorController: GroupIndicatorController,
        errors: ConversationErrors,
        lookupForwardMessage: suspend (String) -> GroupMessage?
    ) {
        this.groupId = groupId
        this.scope = scope
        this.conversationState = conversationState
        this.composerState = composerState
        this.indicatorController = indicatorController
        this.errors = errors
        this.lookupForwardMessage = lookupForwardMessage
    }

    fun markConversationRead() {
        scope.launch {
            orchestrator.awaitResult(GroupMessageCommand.MarkConversationRead(groupId)).onFailure {
                logger.error(it) { "Could not mark group conversation as read" }
            }
        }
    }

    fun forwardMessage(
        messageId: String,
        target: ForwardTarget
    ) {
        scope.launch {
            val message = lookupForwardMessage(messageId) ?: return@launch

            orchestrator.awaitResult(ForwardMessage(parts = message.parts, target = target)).onFailure { error ->
                errors.report(error.message ?: "Message could not be forwarded")
            }
        }
    }

    fun onMessageTextChanged(value: String) {
        if (!composerState.value.availability.isInputEnabled) return

        composerController.textChanged(value)
        errors.clear()
        indicatorController.onLocalTextChanged(
            value = value,
            sendsIndicators = conversationState.value.composerState.sendsIndicators
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
                        clearComposerOnSuccess = true,
                        fallbackError = "Message could not be sent"
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
                        clearVoiceOnSuccess = true,
                        fallbackError = "Voice message could not be sent"
                    )
                }.onFailure { error ->
                    errors.report(error.message ?: "Voice message is not ready to send")
                }
        }
    }

    fun shareCurrentLocation(part: MessagePart) {
        transitionLocationShare(LocationShareEvent.LOCATION_CAPTURED)
        sendAttachmentOnly(
            part = part,
            fallbackError = "Location could not be sent",
            isLocationShare = true
        )
    }

    fun sendAttachmentOnly(
        part: MessagePart,
        fallbackError: String,
        isLocationShare: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        dispatchSend(
            parts = listOf(part),
            clearComposerOnSuccess = false,
            fallbackError = fallbackError,
            isLocationShare = isLocationShare,
            onSuccess = onSuccess
        )
    }

    private fun dispatchSend(
        parts: List<MessagePart>,
        clearComposerOnSuccess: Boolean,
        fallbackError: String,
        clearVoiceOnSuccess: Boolean = false,
        isLocationShare: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        val sendAllowed =
            if (isLocationShare) {
                !conversationState.value.isLoading &&
                    conversationState.value.composerState.isSendActionEnabled &&
                    !isSending.value
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
                orchestrator.awaitResult(GroupMessageCommand.SendMessage(groupId, parts, replyTo))
                    .onSuccess {
                        when {
                            clearComposerOnSuccess -> clearComposer()
                            clearVoiceOnSuccess -> {
                                voiceController.finishSent(parts)
                                clearReply()
                            }

                            else -> clearReply()
                        }
                        onSuccess()
                    }
                    .onFailure { error -> errors.report(error.message ?: fallbackError) }
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
        val text = message.textOrNull()?.takeIf(String::isNotBlank) ?: return
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
                orchestrator.awaitResult(GroupMessageCommand.EditMessage(groupId, messageId, text))
                    .onSuccess { clearComposer() }
                    .onFailure { errors.report(it.message ?: "Message could not be edited") }
            } finally {
                composerController.setSending(false)
            }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) =
        executeAction("Reaction could not be sent") {
            orchestrator.awaitResult(GroupMessageCommand.ToggleReaction(groupId, messageId, emoji))
        }

    fun deleteMessage(messageId: String) = executeAction("Message could not be deleted") {
        orchestrator.awaitResult(GroupMessageCommand.DeleteMessage(groupId, messageId))
    }

    fun retryFailedMessage(messageId: String) = executeAction("Message could not be queued again") {
        orchestrator.awaitResult(GroupMessageCommand.RetryMessage(groupId, messageId))
    }

    private fun executeAction(message: String, action: suspend () -> Result<*>) {
        scope.launch {
            action().onFailure { errors.report(it.message ?: message) }
        }
    }

    fun transitionLocationShare(event: LocationShareEvent) =
        composerController.transitionLocation(event)
}
