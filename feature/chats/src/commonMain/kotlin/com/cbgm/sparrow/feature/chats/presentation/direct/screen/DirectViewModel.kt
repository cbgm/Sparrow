package com.cbgm.sparrow.feature.chats.presentation.direct.screen

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
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toOutgoingContactAttachment
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toOutgoingLocationAttachment
import com.cbgm.sparrow.feature.attachments.presentation.mapper.toOutgoingMessageAttachment
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.MarkDirectConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectChatContextUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.QueueDirectMessageUntilAuthorizedUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.RetryDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SendDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SetDirectTypingUseCase
import com.cbgm.sparrow.feature.chats.presentation.direct.mapper.toDirectUiState
import com.cbgm.sparrow.feature.chats.presentation.direct.mapper.withProfilePicture
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectComposerState
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectUiEvent
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectUiState
import com.cbgm.sparrow.feature.contacts.domain.model.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.usecase.AddDeviceContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase
import com.cbgm.sparrow.feature.media.presentation.model.MediaSelection
import com.cbgm.sparrow.feature.safety.domain.usecase.ObserveMessageSafetyAssessmentsUseCase
import com.cbgm.sparrow.feature.safety.presentation.details.mapper.toDetailsRoute
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DirectViewModel(
    savedStateHandle: SavedStateHandle,
    observeChatContext: ObserveDirectChatContextUseCase,
    private val sendMessage: SendDirectMessageUseCase,
    private val queueMessageUntilAuthorized: QueueDirectMessageUntilAuthorizedUseCase,
    private val markConversationRead: MarkDirectConversationReadUseCase,
    private val retryMessage: RetryDirectMessageUseCase,
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
    private val logger = SparrowLog.withTag("DirectViewModel")
    private val messageText = savedStateHandle.getMutableStateFlow(MESSAGE_TEXT_KEY, "")
    private val errorMessage = MutableStateFlow<String?>(null)
    private val isContactTyping = MutableStateFlow(false)
    private val selectedMedia = MutableStateFlow<List<MediaSelection>>(emptyList())
    private val attachmentBytes = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    private val isSending = MutableStateFlow(false)
    private val loadingAttachmentIds = mutableSetOf<String>()
    private var localTypingStopJob: Job? = null
    private var remoteTypingTimeoutJob: Job? = null
    private var isLocalTyping = false

    private val conversationContext = observeChatContext(conversationId, contactId)

    private val composerContext =
        combine(
            messageText,
            errorMessage,
            isContactTyping,
            selectedMedia,
            isSending
        ) { text, error, contactTyping, media, sending ->
            if (!error.isNullOrEmpty()) logger.error { error }
            ComposerContext(text, error, contactTyping, media, sending)
        }

    private val functionalUiState: Flow<DirectUiState> =
        combine(
            conversationContext,
            composerContext,
            attachmentBytes,
            observeMessageSafetyAssessments()
        ) { context, composer, loadedAttachmentBytes, safetyAssessments ->
            toDirectUiState(
                contactId = contactId,
                fallbackContactName = fallbackContactName,
                conversation = context.conversation,
                contact = context.contact,
                handshake = context.handshake,
                setupMode = context.setupMode,
                currentText = composer.text,
                currentError = composer.error,
                contactTyping = composer.contactTyping,
                safetyAssessments = safetyAssessments,
                attachmentBytes = loadedAttachmentBytes
            ).withProfilePicture(context.profilePictureBytes)
                .copy(
                    selectedMedia = composer.media,
                    isSending = composer.isSending
                )
        }

    val uiState: StateFlow<DirectUiState> =
        functionalUiState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue =
                DirectUiState(
                    contactId = contactId,
                    contactName = fallbackContactName,
                    composerState = DirectComposerState.DISABLED
                )
        )

    init {
        observeIncomingTyping()
    }

    fun onUiEvent(event: DirectUiEvent) {
        when (event) {
            is DirectUiEvent.MessageTextChanged -> onMessageTextChanged(event.text)
            DirectUiEvent.SendClicked -> sendCurrentMessage()
            is DirectUiEvent.MediaSelected -> updateMediaSelection(event.media)
            is DirectUiEvent.OpenFilePicker -> navigator.navigateTo(AppRoute.FilePicker(event.sessionId))
            is DirectUiEvent.ShareCurrentLocation -> sendCurrentLocation(event.location.toOutgoingLocationAttachment())
            is DirectUiEvent.ShareContact -> sendContact(event.contact.toOutgoingContactAttachment())
            is DirectUiEvent.AddSharedContact -> addSharedContact(event.contact)
            is DirectUiEvent.AttachmentVisible -> loadAttachment(event.attachmentId)
            is DirectUiEvent.AttachmentError -> errorMessage.value = event.message
            DirectUiEvent.HeaderClicked -> openContactDetails()
            is DirectUiEvent.RetryMessage -> retryFailedMessage(event.messageId)
            is DirectUiEvent.SafetyWarningClicked ->
                navigator.navigateTo(event.warning.toDetailsRoute(event.messageId, contactId))
            DirectUiEvent.VerifyIdentityClicked -> verifyIdentity()
            DirectUiEvent.ShareIdentityClicked -> navigator.navigateTo(AppRoute.ShareIdentity)
            DirectUiEvent.ImportIdentityClicked -> navigator.navigateTo(AppRoute.ImportContact(contactId))
            DirectUiEvent.BackClicked ->
                if (targetMessageId != null) {
                    navigator.popBackStack()
                } else {
                    navigator.popBackStackTo(AppRoute.Main)
                }
        }
    }

    fun stopTyping() {
        localTypingStopJob?.cancel()
        localTypingStopJob = null
        if (!isLocalTyping) return

        isLocalTyping = false
        sendTypingState(isTyping = false)
    }

    fun markConversationRead() {
        viewModelScope.launch {
            markConversationRead(conversationId)
                .onFailure { error -> logger.warn(error) { "Could not mark direct conversation as read" } }
        }
    }

    private fun observeIncomingTyping() {
        viewModelScope.launch {
            observeTyping(contactId).collect { isTyping ->
                remoteTypingTimeoutJob?.cancel()
                isContactTyping.value = isTyping
                if (isTyping) scheduleRemoteTypingTimeout()
            }
        }
    }

    private fun scheduleRemoteTypingTimeout() {
        remoteTypingTimeoutJob =
            viewModelScope.launch {
                delay(REMOTE_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                isContactTyping.value = false
            }
    }

    private fun onMessageTextChanged(value: String) {
        if (!uiState.value.composerState.isInputEnabled) return

        messageText.value = value
        errorMessage.value = null
        localTypingStopJob?.cancel()

        if (value.isBlank()) {
            stopTyping()
            return
        }

        if (!uiState.value.composerState.sendsTypingIndicators) {
            isLocalTyping = false
            return
        }

        if (!isLocalTyping) {
            isLocalTyping = true
            sendTypingState(isTyping = true)
        }
        scheduleLocalTypingTimeout()
    }

    private fun scheduleLocalTypingTimeout() {
        localTypingStopJob =
            viewModelScope.launch {
                delay(LOCAL_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                stopTypingNow()
            }
    }

    private fun sendCurrentMessage() {
        val composerState = uiState.value.composerState
        if (!composerState.isSendActionEnabled || isSending.value) return

        val text = messageText.value.trim()
        val selections = selectedMedia.value
        if (text.isEmpty() && selections.isEmpty()) return

        errorMessage.value = null
        viewModelScope.launch {
            isSending.value = true
            val attachments = selections.map(MediaSelection::toOutgoingMessageAttachment)
            try {
                when (composerState) {
                    DirectComposerState.REINVITE_REQUIRED -> queueMessageAndStartReinvite(text, attachments)
                    DirectComposerState.REINVITE_PENDING -> queueMessageForPendingReinvite(text, attachments)
                    DirectComposerState.READY -> sendAuthorizedMessage(text, attachments)
                    DirectComposerState.DISABLED -> Unit
                }
            } finally {
                isSending.value = false
            }
        }
    }

    private suspend fun queueMessageAndStartReinvite(
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        clearComposerOnSuccess: Boolean = true
    ) {
        queueMessageUntilAuthorized(conversationId, text, attachments)
            .onSuccess {
                if (clearComposerOnSuccess) clearComposer()
                ensureIdentityExchangeStarted(contactId)
                    .onFailure { error ->
                        errorMessage.value = error.message ?: "Contact invitation could not be started"
                    }
            }.onFailure { error ->
                errorMessage.value = error.message ?: "Message could not be queued"
            }
    }

    private suspend fun queueMessageForPendingReinvite(
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        clearComposerOnSuccess: Boolean = true
    ) {
        queueMessageUntilAuthorized(conversationId, text, attachments)
            .onSuccess { if (clearComposerOnSuccess) clearComposer() }
            .onFailure { error ->
                errorMessage.value = error.message ?: "Message could not be queued"
            }
    }

    private suspend fun sendAuthorizedMessage(
        text: String,
        attachments: List<OutgoingMessageAttachment>,
        clearComposerOnSuccess: Boolean = true
    ) {
        val authorizationError = requireDirectChatAuthorization(contactId).exceptionOrNull()
        if (authorizationError != null) {
            if (
                uiState.value.identitySetupMode == DirectIdentitySetupMode.AUTOMATIC_INVITATION &&
                authorizationError is DirectChatAuthorizationRequiredException
            ) {
                queueMessageAndStartReinvite(text, attachments, clearComposerOnSuccess)
            } else {
                errorMessage.value = authorizationError.message ?: "Message could not be sent"
            }
            return
        }

        sendMessage(conversationId, text, attachments)
            .onSuccess { if (clearComposerOnSuccess) clearComposer() }
            .onFailure { error -> errorMessage.value = error.message ?: "Message could not be sent" }
    }

    private fun sendCurrentLocation(locationAttachment: OutgoingMessageAttachment) {
        val composerState = uiState.value.composerState
        if (!composerState.isSendActionEnabled || isSending.value) return

        errorMessage.value = null
        viewModelScope.launch {
            isSending.value = true
            try {
                val attachments = listOf(locationAttachment)
                when (composerState) {
                    DirectComposerState.REINVITE_REQUIRED ->
                        queueMessageAndStartReinvite(
                            text = "",
                            attachments = attachments,
                            clearComposerOnSuccess = false
                        )

                    DirectComposerState.REINVITE_PENDING ->
                        queueMessageForPendingReinvite(
                            text = "",
                            attachments = attachments,
                            clearComposerOnSuccess = false
                        )

                    DirectComposerState.READY ->
                        sendAuthorizedMessage(
                            text = "",
                            attachments = attachments,
                            clearComposerOnSuccess = false
                        )

                    DirectComposerState.DISABLED -> Unit
                }
            } finally {
                isSending.value = false
            }
        }
    }

    private fun sendContact(contactAttachment: OutgoingMessageAttachment) {
        val composerState = uiState.value.composerState
        if (!composerState.isSendActionEnabled || isSending.value) return

        errorMessage.value = null
        viewModelScope.launch {
            isSending.value = true
            try {
                val attachments = listOf(contactAttachment)
                when (composerState) {
                    DirectComposerState.REINVITE_REQUIRED ->
                        queueMessageAndStartReinvite(
                            text = "",
                            attachments = attachments,
                            clearComposerOnSuccess = false
                        )

                    DirectComposerState.REINVITE_PENDING ->
                        queueMessageForPendingReinvite(
                            text = "",
                            attachments = attachments,
                            clearComposerOnSuccess = false
                        )

                    DirectComposerState.READY ->
                        sendAuthorizedMessage(
                            text = "",
                            attachments = attachments,
                            clearComposerOnSuccess = false
                        )

                    DirectComposerState.DISABLED -> Unit
                }
            } finally {
                isSending.value = false
            }
        }
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
                AddDeviceContactResult.AlreadyExists -> errorMessage.value = null

                AddDeviceContactResult.PermissionDenied ->
                    errorMessage.value = "Contacts permission is required to add this contact"

                AddDeviceContactResult.InvalidPhoneNumber ->
                    errorMessage.value = "The shared phone number is invalid"

                is AddDeviceContactResult.Failure ->
                    errorMessage.value = result.throwable.message ?: "Contact could not be added"
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
            errorMessage.value = null
        }.onFailure { error ->
            errorMessage.value = error.message ?: "Selected attachments could not be attached"
        }
    }

    private fun loadAttachment(attachmentId: String) {
        if (attachmentId.isBlank() || attachmentBytes.value.containsKey(attachmentId)) return
        if (!loadingAttachmentIds.add(attachmentId)) return

        viewModelScope.launch {
            loadMessageAttachment(attachmentId)
                .onSuccess { bytes -> attachmentBytes.value = attachmentBytes.value + (attachmentId to bytes) }
                .onFailure { error -> logger.warn(error) { "Could not load message attachment $attachmentId" } }
            loadingAttachmentIds.remove(attachmentId)
        }
    }

    private suspend fun clearComposer() {
        messageText.value = ""
        selectedMedia.value = emptyList()
        stopTypingNow()
    }

    private fun retryFailedMessage(messageId: String) {
        if (messageId.isBlank()) return
        errorMessage.value = null
        viewModelScope.launch {
            retryMessage(messageId)
                .onFailure { error -> errorMessage.value = error.message ?: "Message could not be queued again" }
        }
    }

    private fun sendTypingState(isTyping: Boolean) {
        viewModelScope.launch { sendTypingStateNow(isTyping) }
    }

    private suspend fun sendTypingStateNow(isTyping: Boolean) {
        setTyping(contactId, isTyping)
            .onFailure { error -> logger.warn(error) { "Could not send direct typing state for $contactId" } }
    }

    private suspend fun stopTypingNow() {
        if (!isLocalTyping) return
        isLocalTyping = false
        sendTypingStateNow(isTyping = false)
    }

    private fun openContactDetails() {
        navigator.navigateTo(AppRoute.ContactDetails(conversationId, contactId))
    }

    private fun verifyIdentity() {
        navigator.navigateTo(AppRoute.ContactDetails(conversationId, contactId, openVerification = true))
    }

    private data class ComposerContext(
        val text: String,
        val error: String?,
        val contactTyping: Boolean,
        val media: List<MediaSelection>,
        val isSending: Boolean
    )

    private companion object {
        const val MESSAGE_TEXT_KEY = "messageText"
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
