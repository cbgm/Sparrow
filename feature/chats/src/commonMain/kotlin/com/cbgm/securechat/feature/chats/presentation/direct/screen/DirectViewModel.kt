package com.cbgm.securechat.feature.chats.presentation.direct.screen

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.chats.domain.model.direct.DirectConversation
import com.cbgm.securechat.feature.chats.domain.usecase.direct.MarkDirectConversationReadUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.ObserveDirectConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.ObserveDirectTypingUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.RefreshDirectDeliveryStateUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.RetryDirectMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.SendDirectMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.direct.SetDirectTypingUseCase
import com.cbgm.securechat.feature.chats.presentation.direct.mapper.isDirectChatAuthorized
import com.cbgm.securechat.feature.chats.presentation.direct.mapper.resolveContactName
import com.cbgm.securechat.feature.chats.presentation.direct.mapper.toSecurityState
import com.cbgm.securechat.feature.chats.presentation.direct.mapper.toUiModel
import com.cbgm.securechat.feature.chats.presentation.direct.model.DirectUiEvent
import com.cbgm.securechat.feature.chats.presentation.direct.model.DirectUiState
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.securechat.feature.contacts.domain.usecase.EnsureIdentityExchangeStarted
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveIdentityHandshakeState
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveIdentitySetupMode
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
    private val conversationId: String,
    private val contactId: String,
    private val fallbackContactName: String,
    observeConversation: ObserveDirectConversationUseCase,
    private val sendMessage: SendDirectMessageUseCase,
    private val markConversationRead: MarkDirectConversationReadUseCase,
    private val retryMessage: RetryDirectMessageUseCase,
    private val refreshDeliveryState: RefreshDirectDeliveryStateUseCase,
    observeIdentitySetupMode: ObserveIdentitySetupMode,
    private val ensureIdentityExchangeStarted: EnsureIdentityExchangeStarted,
    observeIdentityHandshakeState: ObserveIdentityHandshakeState,
    observeContact: ObserveContact,
    private val observeTyping: ObserveDirectTypingUseCase,
    private val setTyping: SetDirectTypingUseCase
) : BaseViewModel() {
    private val logger = SecureChatLog.withTag("DirectViewModel")
    private val messageText = MutableStateFlow("")
    private val errorMessage = MutableStateFlow<String?>(null)
    private val isContactTyping = MutableStateFlow(false)
    private var localTypingStopJob: Job? = null
    private var remoteTypingTimeoutJob: Job? = null
    private var isLocalTyping = false

    private val conversationFlow: Flow<DirectConversation?> = observeConversation(conversationId)
    private val contactFlow: Flow<Contact?> = observeContact(contactId = contactId)
    private val identityHandshakeFlow: Flow<IdentityHandshakeState?> = observeIdentityHandshakeState(contactId)
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

    val uiState: StateFlow<DirectUiState> =
        combine(
            conversationContext,
            messageText,
            errorMessage,
            isContactTyping
        ) { context, text, error, contactTyping ->
            context.toUiState(text, error, contactTyping)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue =
                DirectUiState(
                    contactId = contactId,
                    contactName = fallbackContactName,
                    isMessageInputEnabled = false
                )
        )

    init {
        observeAutomaticIdentitySetup()
        observeIncomingTyping()
        observeDeliveryTimeouts()
    }

    fun onUiEvent(event: DirectUiEvent) {
        when (event) {
            is DirectUiEvent.MessageTextChanged -> onMessageTextChanged(event.text)
            DirectUiEvent.SendClicked -> sendCurrentMessage()
            DirectUiEvent.HeaderClicked -> openContactDetails()
            is DirectUiEvent.RetryMessage -> retryFailedMessage(event.messageId)
            DirectUiEvent.VerifyIdentityClicked -> verifyIdentity()
            DirectUiEvent.ManualIdentitySetupClicked -> Unit
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

    private fun ConversationContext.toUiState(
        currentText: String,
        currentError: String?,
        contactTyping: Boolean
    ): DirectUiState =
        DirectUiState(
            contactId = contactId,
            contactName = resolveContactName(contact, fallbackContactName),
            messages = conversation?.messages.orEmpty().asReversed().map { it.toUiModel() },
            messageText = currentText,
            isContactTyping = contactTyping,
            contactSecurityState = contact.toSecurityState(),
            identityHandshakeState = handshake,
            identitySetupMode = setupMode,
            isLoading = contact == null,
            isMessageInputEnabled = isDirectChatAuthorized(contact, handshake, setupMode),
            errorMessage = currentError
        )

    private fun observeAutomaticIdentitySetup() {
        viewModelScope.launch {
            identitySetupModeFlow.collect { mode ->
                if (mode != DirectIdentitySetupMode.AUTOMATIC_INVITATION) return@collect

                ensureIdentityExchangeStarted(contactId)
                    .onFailure { error ->
                        errorMessage.value = error.message ?: "Contact invitation could not be started"
                    }
            }
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

    private fun observeDeliveryTimeouts() {
        viewModelScope.launch {
            while (true) {
                refreshDeliveryState(conversationId)
                delay(DELIVERY_REFRESH_INTERVAL_MILLISECONDS.milliseconds)
            }
        }
    }

    private fun onMessageTextChanged(value: String) {
        if (!uiState.value.isMessageInputEnabled) return

        messageText.value = value
        errorMessage.value = null
        localTypingStopJob?.cancel()

        if (value.isBlank()) {
            stopTyping()
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
        if (!uiState.value.isMessageInputEnabled) return
        val text = messageText.value.trim()
        if (text.isEmpty()) return

        messageText.value = ""
        errorMessage.value = null
        stopTyping()
        viewModelScope.launch {
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
        const val DELIVERY_REFRESH_INTERVAL_MILLISECONDS = 15_000L
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
