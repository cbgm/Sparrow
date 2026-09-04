package com.cbgm.sparrow.feature.chats.presentation.direct

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.attachments.domain.model.MessageAttachmentPolicy
import com.cbgm.sparrow.feature.attachments.domain.model.OutgoingMessageAttachment
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.attachments.domain.usecase.LoadMessageAttachmentUseCase
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toOutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareEvent
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareState
import com.cbgm.sparrow.feature.chats.domain.model.LocationShareStateMachine
import com.cbgm.sparrow.feature.chats.domain.model.MessageComposerPolicy
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectComposerState
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DeleteDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.EditDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.MarkDirectConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.QueueDirectMessageUntilAuthorizedUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.RetryDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SendDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SetDirectTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ToggleDirectMessageReactionUseCase
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageComposerUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageContextUiState
import com.cbgm.sparrow.feature.chats.presentation.component.model.TypingUiState
import com.cbgm.sparrow.feature.chats.presentation.direct.mapper.toDirectConversationUiState
import com.cbgm.sparrow.feature.chats.presentation.direct.mapper.toDirectReplyPreview
import com.cbgm.sparrow.feature.chats.presentation.direct.mapper.withProfilePicture
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiEvent
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectConversationUiState
import com.cbgm.sparrow.feature.contacts.domain.model.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.usecase.AddDeviceContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyAssessmentsUseCase
import com.cbgm.sparrow.feature.safety.presentation.details.mapper.toMessageSafetyDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DirectConversationViewModel(
    savedStateHandle: SavedStateHandle,
    observeChatContext: ObserveDirectChatContextUseCase,
    private val sendMessage: SendDirectMessageUseCase,
    private val queueMessageUntilAuthorized: QueueDirectMessageUntilAuthorizedUseCase,
    private val markConversationRead: MarkDirectConversationReadUseCase,
    private val retryMessage: RetryDirectMessageUseCase,
    private val toggleMessageReaction: ToggleDirectMessageReactionUseCase,
    private val deleteMessageUseCase: DeleteDirectMessageUseCase,
    private val editMessageUseCase: EditDirectMessageUseCase,
    private val ensureIdentityExchangeStarted: EnsureIdentityExchangeStartedUseCase,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    private val observeTyping: ObserveDirectTypingUseCase,
    private val setTyping: SetDirectTypingUseCase,
    observeMessageSafetyAssessments: ObserveMessageSafetyAssessmentsUseCase,
    private val loadMessageAttachment: LoadMessageAttachmentUseCase,
    private val addDeviceContact: AddDeviceContactUseCase
) : BaseViewModel() {
    private val conversationId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.Chat::conversationId.name)
    private val contactId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.Chat::contactId.name)
    private val fallbackContactName =
        savedStateHandle.requireRouteArgument<String>(AppRoute.Chat::contactName.name)
    private val targetMessageId =
        savedStateHandle.get<String>(AppRoute.Chat::targetMessageId.name)
    private val logger = SparrowLog.withTag("DirectConversationViewModel")
    private val messageText = savedStateHandle.getMutableStateFlow(MESSAGE_TEXT_KEY, "")
    private val replyToMessageId = savedStateHandle.getMutableStateFlow(REPLY_TO_MESSAGE_ID_KEY, "")
    private val editingMessageId = savedStateHandle.getMutableStateFlow(EDITING_MESSAGE_ID_KEY, "")
    private val mutableErrorMessage = MutableStateFlow<String?>(null)
    private val selectedMedia = MutableStateFlow<List<MediaSelection>>(emptyList())
    private val attachmentBytes = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    private val isSending = MutableStateFlow(false)
    private val contextMessageId = MutableStateFlow<String?>(null)
    private val locationShareState = MutableStateFlow(LocationShareState.IDLE)
    private val loadingAttachmentIds = mutableSetOf<String>()

    private val conversationContext = observeChatContext(conversationId, contactId)

    private val typingController =
        TypingIndicatorController(
            scope = viewModelScope,
            sendTypingState = { isTyping -> setTyping(contactId, isTyping) },
            logTag = "DirectConversationViewModel"
        )

    private val composerDraft =
        combine(messageText, replyToMessageId, editingMessageId) { text, replyId, editId ->
            ComposerDraft(
                text = text,
                replyToMessageId = replyId.takeIf(String::isNotBlank),
                editingMessageId = editId.takeIf(String::isNotBlank)
            )
        }

    private val composerRuntime =
        combine(selectedMedia, isSending, locationShareState) { media, sending, locationState ->
            ComposerRuntime(
                media = media,
                isSending = sending,
                locationShareState = locationState
            )
        }

    val conversationState: StateFlow<DirectConversationUiState> =
        combine(
            conversationContext,
            attachmentBytes,
            observeMessageSafetyAssessments()
        ) { context, loadedAttachmentBytes, safetyAssessments ->
            toDirectConversationUiState(
                contactId = contactId,
                fallbackContactName = fallbackContactName,
                conversation = context.conversation,
                contact = context.contact,
                handshake = context.handshake,
                setupMode = context.setupMode,
                safetyAssessments = safetyAssessments,
                attachmentBytes = loadedAttachmentBytes
            ).withProfilePicture(context.profilePictureBytes)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue =
                DirectConversationUiState(
                    contactId = contactId,
                    contactName = fallbackContactName,
                    composerState = DirectComposerState.DISABLED
                )
        )

    val composerState: StateFlow<MessageComposerUiState> =
        combine(
            conversationState,
            conversationContext,
            composerDraft,
            composerRuntime
        ) { conversation, context, draft, runtime ->
            val availability =
                MessageComposerPolicy.resolve(
                    isInputAllowed = !conversation.isLoading && conversation.composerState.isInputEnabled,
                    isSendAllowed = !conversation.isLoading && conversation.composerState.isSendActionEnabled,
                    isSending = runtime.isSending,
                    isEditing = draft.editingMessageId != null,
                    selectedAttachmentCount = runtime.media.size,
                    locationShareState = runtime.locationShareState
                )

            MessageComposerUiState(
                messageText = draft.text,
                replyTo =
                    draft.replyToMessageId.toDirectReplyPreview(
                        conversation = context.conversation,
                        contactName = conversation.contactName
                    ),
                editingMessageId = draft.editingMessageId,
                selectedMedia = runtime.media,
                isSending = runtime.isSending,
                locationShareState = runtime.locationShareState,
                availability = availability
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = MessageComposerUiState()
        )

    val contextState: StateFlow<MessageContextUiState<MessageBubbleUi>> =
        combine(conversationState, contextMessageId) { conversation, selectedMessageId ->
            val message = conversation.messages.firstOrNull { it.id == selectedMessageId }
            MessageContextUiState(
                message = message,
                canEdit = message?.canEdit == true
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = MessageContextUiState()
        )

    val typingState: StateFlow<TypingUiState> =
        combine(typingController.isContactTyping, conversationState) { isTyping, conversation ->
            TypingUiState(
                isTyping = isTyping,
                displayName = conversation.contactName
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = TypingUiState(displayName = fallbackContactName)
        )

    val errorMessage: StateFlow<String?> = mutableErrorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            observeTyping(contactId).collect(typingController::onIncomingTypingChanged)
        }
    }

    fun onUiEvent(event: DirectConversationUiEvent) {
        when (event) {
            is DirectConversationUiEvent.MessageTextChanged -> onMessageTextChanged(event.text)
            DirectConversationUiEvent.SendClicked -> sendCurrentMessage()
            is DirectConversationUiEvent.ReplyToMessage -> startReply(event.messageId)
            DirectConversationUiEvent.CancelReply -> clearReply()
            is DirectConversationUiEvent.EditMessage -> startEdit(event.messageId)
            is DirectConversationUiEvent.MessageContextRequested -> contextMessageId.value = event.messageId
            DirectConversationUiEvent.MessageContextDismissed -> contextMessageId.value = null
            DirectConversationUiEvent.CancelEdit -> cancelEdit()
            is DirectConversationUiEvent.MessageReactionSelected -> toggleReaction(event.messageId, event.emoji)
            is DirectConversationUiEvent.DeleteMessage -> deleteMessage(event.messageId)
            is DirectConversationUiEvent.MediaSelected -> updateMediaSelection(event.media)
            is DirectConversationUiEvent.OpenFilePicker -> navigator.navigateTo(AppRoute.FilePicker(event.sessionId))
            DirectConversationUiEvent.LocationCaptureStarted -> transitionLocationShare(LocationShareEvent.CAPTURE_STARTED)
            is DirectConversationUiEvent.ShareCurrentLocation -> shareCurrentLocation(event.location.toOutgoingMessageAttachment())
            is DirectConversationUiEvent.LocationCaptureFailed -> {
                transitionLocationShare(LocationShareEvent.FAILED)
                setError(event.message)
            }
            is DirectConversationUiEvent.ShareContact -> sendAttachmentOnly(event.contact.toOutgoingMessageAttachment())
            is DirectConversationUiEvent.AddSharedContact -> addSharedContact(event.contact)
            is DirectConversationUiEvent.AttachmentVisible -> loadAttachment(event.attachmentId)
            is DirectConversationUiEvent.AttachmentError -> setError(event.message)
            DirectConversationUiEvent.HeaderClicked -> openContactDetails()
            is DirectConversationUiEvent.RetryMessage -> retryFailedMessage(event.messageId)
            is DirectConversationUiEvent.SafetyWarningClicked ->
                navigator.navigateTo(event.warning.toMessageSafetyDetails(event.messageId, contactId))
            DirectConversationUiEvent.VerifyIdentityClicked -> verifyIdentity()
            DirectConversationUiEvent.ShareIdentityClicked -> navigator.navigateTo(AppRoute.ShareIdentity)
            DirectConversationUiEvent.ImportIdentityClicked -> navigator.navigateTo(AppRoute.ImportContact(contactId))
            DirectConversationUiEvent.BackClicked ->
                if (targetMessageId != null) {
                    navigator.popBackStack()
                } else {
                    navigator.popBackStackTo(AppRoute.Main)
                }
        }
    }

    fun stopTyping() = typingController.stopLocalTyping()

    fun markConversationRead() {
        viewModelScope.launch {
            markConversationRead(conversationId)
                .onFailure { error -> logger.warn(error) { "Could not mark direct conversation as read" } }
        }
    }

    private fun onMessageTextChanged(value: String) {
        val directComposerState = conversationState.value.composerState
        if (!composerState.value.availability.isInputEnabled) return

        messageText.value = value
        clearError()
        typingController.onLocalTextChanged(value, sendsIndicators = directComposerState.sendsTypingIndicators)
    }

    private fun sendCurrentMessage() {
        val text = messageText.value.trim()
        val editMessageId = editingMessageId.value.takeIf(String::isNotBlank)
        if (editMessageId != null) {
            if (text.isEmpty()) return
            editCurrentMessage(editMessageId, text)
            return
        }

        val selections = selectedMedia.value
        if (text.isEmpty() && selections.isEmpty()) return

        dispatchSend(
            text = text,
            attachments = selections.map(MediaSelection::toOutgoingMessageAttachment),
            clearComposerOnSuccess = true
        )
    }

    private fun shareCurrentLocation(attachment: OutgoingMessageAttachment) {
        transitionLocationShare(LocationShareEvent.LOCATION_CAPTURED)
        sendAttachmentOnly(attachment, isLocationShare = true)
    }

    private fun sendAttachmentOnly(
        attachment: OutgoingMessageAttachment,
        isLocationShare: Boolean = false
    ) {
        dispatchSend(
            text = "",
            attachments = listOf(attachment),
            clearComposerOnSuccess = false,
            isLocationShare = isLocationShare
        )
    }

    private fun dispatchSend(
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        clearComposerOnSuccess: Boolean,
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
        clearError()
        viewModelScope.launch {
            if (isLocationShare) transitionLocationShare(LocationShareEvent.SEND_STARTED)
            isSending.value = true
            try {
                when (directComposerState) {
                    DirectComposerState.REINVITE_REQUIRED ->
                        queueMessageAndStartReinvite(text, attachments, replyTo, clearComposerOnSuccess)

                    DirectComposerState.REINVITE_PENDING ->
                        queueMessageForPendingReinvite(text, attachments, replyTo, clearComposerOnSuccess)

                    DirectComposerState.READY ->
                        sendAuthorizedMessage(text, attachments, replyTo, clearComposerOnSuccess)

                    DirectComposerState.DISABLED -> Unit
                }
            } finally {
                isSending.value = false
                if (isLocationShare) transitionLocationShare(LocationShareEvent.COMPLETED)
            }
        }
    }

    private suspend fun queueMessageAndStartReinvite(
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        replyToMessageId: String?,
        clearComposerOnSuccess: Boolean = true
    ) {
        queueMessageUntilAuthorized(conversationId, text, attachments, replyToMessageId)
            .onSuccess {
                if (clearComposerOnSuccess) clearComposer() else clearReply()
                ensureIdentityExchangeStarted(contactId)
                    .onFailure { error ->
                        setError(error.message ?: "Contact invitation could not be started")
                    }
            }.onFailure { error ->
                setError(error.message ?: "Message could not be queued")
            }
    }

    private suspend fun queueMessageForPendingReinvite(
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        replyToMessageId: String?,
        clearComposerOnSuccess: Boolean = true
    ) {
        queueMessageUntilAuthorized(conversationId, text, attachments, replyToMessageId)
            .onSuccess { if (clearComposerOnSuccess) clearComposer() else clearReply() }
            .onFailure { error -> setError(error.message ?: "Message could not be queued") }
    }

    private suspend fun sendAuthorizedMessage(
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        replyToMessageId: String?,
        clearComposerOnSuccess: Boolean = true
    ) {
        val authorizationError = requireDirectChatAuthorization(contactId).exceptionOrNull()
        if (authorizationError != null) {
            if (
                conversationState.value.identitySetupMode == DirectIdentitySetupMode.AUTOMATIC_INVITATION &&
                authorizationError is DirectChatAuthorizationRequiredException
            ) {
                queueMessageAndStartReinvite(text, attachments, replyToMessageId, clearComposerOnSuccess)
            } else {
                setError(authorizationError.message ?: "Message could not be sent")
            }
            return
        }

        sendMessage(conversationId, text, attachments, replyToMessageId)
            .onSuccess { if (clearComposerOnSuccess) clearComposer() else clearReply() }
            .onFailure { error -> setError(error.message ?: "Message could not be sent") }
    }

    private fun addSharedContact(contact: SharedContact) {
        viewModelScope.launch {
            when (
                val result =
                    addDeviceContact(
                        displayName = contact.displayName,
                        phoneNumber = contact.phoneNumber
                    )
            ) {
                AddDeviceContactResult.Added,
                AddDeviceContactResult.AlreadyExists -> clearError()

                AddDeviceContactResult.PermissionDenied ->
                    setError("Contacts permission is required to add this contact")

                AddDeviceContactResult.InvalidPhoneNumber ->
                    setError("The shared phone number is invalid")

                is AddDeviceContactResult.Failure ->
                    setError(result.throwable.message ?: "Contact could not be added")
            }
        }
    }

    private fun updateMediaSelection(media: List<MediaSelection>) {
        runCatching {
            MessageAttachmentPolicy.requireValid(
                media.map(MediaSelection::toOutgoingMessageAttachment)
            )
        }.onSuccess {
            selectedMedia.value = media
            clearError()
        }.onFailure { error ->
            setError(error.message ?: "Selected attachments could not be attached")
        }
    }

    private fun loadAttachment(attachmentId: String) {
        if (attachmentId.isBlank() || attachmentBytes.value.containsKey(attachmentId)) return
        if (!loadingAttachmentIds.add(attachmentId)) return

        viewModelScope.launch {
            loadMessageAttachment(attachmentId)
                .onSuccess { bytes -> attachmentBytes.value += (attachmentId to bytes) }
                .onFailure { error -> logger.warn(error) { "Could not load message attachment $attachmentId" } }
            loadingAttachmentIds.remove(attachmentId)
        }
    }

    private suspend fun clearComposer() {
        messageText.value = ""
        replyToMessageId.value = ""
        editingMessageId.value = ""
        selectedMedia.value = emptyList()
        typingController.stopLocalTypingNow()
    }

    private fun startReply(messageId: String) {
        if (conversationState.value.messages.none { message -> message.id == messageId }) return
        editingMessageId.value = ""
        replyToMessageId.value = messageId
        clearError()
    }

    private fun clearReply() {
        replyToMessageId.value = ""
    }

    private fun startEdit(messageId: String) {
        val message = conversationState.value.messages.firstOrNull { it.id == messageId } ?: return
        if (!message.canEdit) return

        val text = message.textPart?.text?.takeIf(String::isNotBlank) ?: return

        replyToMessageId.value = ""
        selectedMedia.value = emptyList()
        editingMessageId.value = messageId
        messageText.value = text
        clearError()
    }

    private fun cancelEdit() {
        editingMessageId.value = ""
        messageText.value = ""
        clearError()
    }

    private fun editCurrentMessage(messageId: String, text: String) {
        if (isSending.value) return
        clearError()
        viewModelScope.launch {
            isSending.value = true
            try {
                editMessageUseCase(conversationId, messageId, text)
                    .onSuccess { clearComposer() }
                    .onFailure { error -> setError(error.message ?: "Message could not be edited") }
            } finally {
                isSending.value = false
            }
        }
    }

    private fun toggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            toggleMessageReaction(conversationId, messageId, emoji)
                .onFailure { error -> setError(error.message ?: "Reaction could not be sent") }
        }
    }

    private fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            deleteMessageUseCase(conversationId, messageId)
                .onFailure { error -> setError(error.message ?: "Message could not be deleted") }
        }
    }

    private fun retryFailedMessage(messageId: String) {
        if (messageId.isBlank()) return
        clearError()
        viewModelScope.launch {
            retryMessage(messageId)
                .onFailure { error -> setError(error.message ?: "Message could not be queued again") }
        }
    }

    private fun transitionLocationShare(event: LocationShareEvent) {
        locationShareState.value =
            LocationShareStateMachine.transition(
                state = locationShareState.value,
                event = event
            )
    }

    private fun setError(message: String) {
        logger.error { message }
        mutableErrorMessage.value = message
    }

    private fun clearError() {
        mutableErrorMessage.value = null
    }

    private fun openContactDetails() {
        navigator.navigateTo(AppRoute.ContactDetails(conversationId, contactId))
    }

    private fun verifyIdentity() {
        navigator.navigateTo(AppRoute.ContactDetails(conversationId, contactId, openVerification = true))
    }

    private data class ComposerDraft(
        val text: String,
        val replyToMessageId: String?,
        val editingMessageId: String?
    )

    private data class ComposerRuntime(
        val media: List<MediaSelection>,
        val isSending: Boolean,
        val locationShareState: LocationShareState
    )

    private companion object {
        const val MESSAGE_TEXT_KEY = "messageText"
        const val REPLY_TO_MESSAGE_ID_KEY = "replyToMessageId"
        const val EDITING_MESSAGE_ID_KEY = "editingMessageId"
    }
}
