package com.cbgm.sparrow.feature.chats.presentation.direct.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.security.DirectIdentitySetupMode
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.navigation.requireRouteArgument
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.MarkDirectConversationReadUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.ObserveDirectTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.QueueDirectMessageUntilAuthorizedUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.RetryDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SendDirectMessageUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.SetDirectTypingUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.profile.ObserveRemoteProfilePicturesUseCase
import com.cbgm.sparrow.feature.chats.presentation.direct.mapper.toDirectUiState
import com.cbgm.sparrow.feature.chats.presentation.direct.mapper.withProfilePicture
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectComposerState
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectUiEvent
import com.cbgm.sparrow.feature.chats.presentation.direct.model.DirectUiState
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.model.DirectChatAuthorizationRequiredException
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.sparrow.feature.contacts.domain.usecase.EnsureIdentityExchangeStartedUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveContactUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentityHandshakeStateUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.ObserveIdentitySetupModeUseCase
import com.cbgm.sparrow.feature.contacts.domain.usecase.RequireDirectChatAuthorizationUseCase
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
    observeConversation: ObserveDirectConversationUseCase,
    private val sendMessage: SendDirectMessageUseCase,
    private val queueMessageUntilAuthorized: QueueDirectMessageUntilAuthorizedUseCase,
    private val markConversationRead: MarkDirectConversationReadUseCase,
    private val retryMessage: RetryDirectMessageUseCase,
    observeIdentitySetupMode: ObserveIdentitySetupModeUseCase,
    private val ensureIdentityExchangeStarted: EnsureIdentityExchangeStartedUseCase,
    private val requireDirectChatAuthorization: RequireDirectChatAuthorizationUseCase,
    observeIdentityHandshakeState: ObserveIdentityHandshakeStateUseCase,
    observeContact: ObserveContactUseCase,
    observeProfilePictures: ObserveRemoteProfilePicturesUseCase,
    private val observeTyping: ObserveDirectTypingUseCase,
    private val setTyping: SetDirectTypingUseCase
) : BaseViewModel() {
    private val conversationId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.Chat::conversationId.name)
    private val contactId =
        savedStateHandle.requireRouteArgument<String>(AppRoute.Chat::contactId.name)
    private val fallbackContactName =
        savedStateHandle.requireRouteArgument<String>(AppRoute.Chat::contactName.name)
    private val logger = SparrowLog.withTag("DirectViewModel")
    private val messageText = savedStateHandle.getMutableStateFlow(MESSAGE_TEXT_KEY, "")
    private val errorMessage = MutableStateFlow<String?>(null)
    private val isContactTyping = MutableStateFlow(false)
    private var localTypingStopJob: Job? = null
    private var remoteTypingTimeoutJob: Job? = null
    private var isLocalTyping = false

    private val conversationFlow: Flow<DirectConversation?> = observeConversation(conversationId)
    private val contactFlow: Flow<Contact?> = observeContact(contactId = contactId)
    private val identityHandshakeFlow: Flow<IdentityHandshakeState?> = observeIdentityHandshakeState(contactId)
    private val profilePictureFlow: Flow<ByteArray?> = observeProfilePictures(contactId)
    private val identitySetupModeFlow: StateFlow<DirectIdentitySetupMode> =
        observeIdentitySetupMode()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING
            )

    private val conversationContext =
        combine(
            conversationFlow,
            contactFlow,
            identityHandshakeFlow,
            identitySetupModeFlow
        ) { conversation, contact, handshake, setupMode ->
            ConversationContext(conversation, contact, handshake, setupMode)
        }

    private val functionalUiState: Flow<DirectUiState> =
        combine(
            conversationContext,
            messageText,
            errorMessage,
            isContactTyping
        ) { context, text, error, contactTyping ->
            toDirectUiState(
                contactId = contactId,
                fallbackContactName = fallbackContactName,
                conversation = context.conversation,
                contact = context.contact,
                handshake = context.handshake,
                setupMode = context.setupMode,
                currentText = text,
                currentError = error,
                contactTyping = contactTyping
            )
        }

    val uiState: StateFlow<DirectUiState> =
        combine(functionalUiState, profilePictureFlow) { state, profilePictureBytes ->
            state.withProfilePicture(profilePictureBytes)
        }.stateIn(
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
            DirectUiEvent.HeaderClicked -> openContactDetails()
            is DirectUiEvent.RetryMessage -> retryFailedMessage(event.messageId)
            DirectUiEvent.VerifyIdentityClicked -> verifyIdentity()
            DirectUiEvent.ShareIdentityClicked -> navigator.navigateTo(AppRoute.ShareIdentity)
            DirectUiEvent.ImportIdentityClicked -> navigator.navigateTo(AppRoute.ImportContact(contactId))
            DirectUiEvent.BackClicked -> navigator.popBackStackTo(AppRoute.Main)
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
        if (!composerState.isSendActionEnabled) return

        val text = messageText.value.trim()
        if (text.isEmpty()) return

        errorMessage.value = null
        when (composerState) {
            DirectComposerState.REINVITE_REQUIRED -> queueMessageAndStartReinvite(text)
            DirectComposerState.REINVITE_PENDING -> queueMessageForPendingReinvite(text)
            DirectComposerState.READY -> sendAuthorizedMessage(text)
            DirectComposerState.DISABLED -> Unit
        }
    }

    private fun queueMessageAndStartReinvite(text: String) {
        viewModelScope.launch {
            queueMessageUntilAuthorized(conversationId, text)
                .onSuccess {
                    messageText.value = ""
                    stopTyping()
                    ensureIdentityExchangeStarted(contactId)
                        .onFailure { error ->
                            errorMessage.value = error.message ?: "Contact invitation could not be started"
                        }
                }.onFailure { error ->
                    errorMessage.value = error.message ?: "Message could not be queued"
                }
        }
    }

    private fun queueMessageForPendingReinvite(text: String) {
        viewModelScope.launch {
            queueMessageUntilAuthorized(conversationId, text)
                .onSuccess {
                    messageText.value = ""
                    stopTyping()
                }.onFailure { error ->
                    errorMessage.value = error.message ?: "Message could not be queued"
                }
        }
    }

    private fun sendAuthorizedMessage(text: String) {
        viewModelScope.launch {
            val authorizationError = requireDirectChatAuthorization(contactId).exceptionOrNull()
            if (authorizationError != null) {
                if (
                    identitySetupModeFlow.value == DirectIdentitySetupMode.AUTOMATIC_INVITATION &&
                    authorizationError is DirectChatAuthorizationRequiredException
                ) {
                    queueMessageAndStartReinvite(text)
                } else {
                    errorMessage.value = authorizationError.message ?: "Message could not be sent"
                }
                return@launch
            }

            messageText.value = ""
            stopTyping()
            sendMessage(conversationId, text)
                .onFailure { error ->
                    messageText.value = text
                    errorMessage.value = error.message ?: "Message could not be sent"
                }
        }
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

    private data class ConversationContext(
        val conversation: DirectConversation?,
        val contact: Contact?,
        val handshake: IdentityHandshakeState?,
        val setupMode: DirectIdentitySetupMode
    )

    private companion object {
        const val MESSAGE_TEXT_KEY = "messageText"
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
