package com.cbgm.securechat.feature.chats.presentation.direct

import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.security.DirectIdentitySetupMode
import com.cbgm.securechat.core.ui.navigation.AppRoute
import com.cbgm.securechat.core.ui.presentation.BaseViewModel
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.usecase.MarkConversationReadUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversationUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveTypingIndicatorUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.RefreshDeliveryStateUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.RetryMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.SendMessageUseCase
import com.cbgm.securechat.feature.chats.domain.usecase.SetTypingIndicatorUseCase
import com.cbgm.securechat.feature.chats.presentation.direct.mapper.isDirectChatAuthorized
import com.cbgm.securechat.feature.chats.presentation.direct.mapper.resolveContactName
import com.cbgm.securechat.feature.chats.presentation.direct.mapper.toSecurityState
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DirectViewModel(
    private val conversationId: String,
    private val contactId: String,
    private val fallbackContactName: String,
    private val observeConversation: ObserveConversationUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markConversationReadUseCase: MarkConversationReadUseCase,
    private val retryFailedMessage: RetryMessageUseCase,
    private val refreshDeliveryState: RefreshDeliveryStateUseCase,
    observeIdentitySetupMode: ObserveIdentitySetupMode,
    private val ensureIdentityExchangeStarted: EnsureIdentityExchangeStarted,
    observeIdentityHandshakeState: ObserveIdentityHandshakeState,
    observeContact: ObserveContact,
    private val observeTypingIndicator: ObserveTypingIndicatorUseCase,
    private val setTypingIndicator: SetTypingIndicatorUseCase
) : BaseViewModel() {
    private val logger = SecureChatLog.withTag("DirectViewModel")

    private val messageText = MutableStateFlow("")

    private val errorMessage = MutableStateFlow<String?>(null)

    private val isContactTyping = MutableStateFlow(false)

    private var localTypingStopJob: Job? = null

    private var remoteTypingTimeoutJob: Job? = null

    private var isLocalTyping = false

    private val contactFlow: Flow<Contact?> =
        observeContact(contactId = contactId)

    private val conversationFlow: Flow<Conversation?> =
        observeConversation(conversationId)

    private val identityHandshakeStateFlow: Flow<IdentityHandshakeState?> =
        observeIdentityHandshakeState(contactId)

    private val directIdentitySetupModeFlow: StateFlow<DirectIdentitySetupMode> =
        observeIdentitySetupMode()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = DirectIdentitySetupMode.MANUAL_IDENTITY_SHARING
            )

    private val chatContentFlow: Flow<ChatContentState> =
        combine(
            conversationFlow,
            contactFlow,
            identityHandshakeStateFlow,
            directIdentitySetupModeFlow
        ) { conversation, contact, identityHandshakeState, directIdentitySetupMode ->
            ChatContentState(
                conversation = conversation,
                contact = contact,
                identityHandshakeState = identityHandshakeState,
                directIdentitySetupMode = directIdentitySetupMode
            )
        }

    private val composerFlow: Flow<ComposerState> =
        combine(
            messageText,
            errorMessage,
            isContactTyping
        ) { currentMessageText, currentError, contactTyping ->

            ComposerState(
                messageText = currentMessageText,
                errorMessage = currentError,
                isContactTyping = contactTyping
            )
        }

    private val screenContentFlow: Flow<ScreenContentState> =
        combine(
            chatContentFlow,
            composerFlow
        ) { chatContent, composer ->

            ScreenContentState(
                chatContent = chatContent,
                composer = composer
            )
        }

    val uiState: StateFlow<DirectUiState> =
        screenContentFlow
            .map { screenContent ->
                val conversation = screenContent.chatContent.conversation

                val contact = screenContent.chatContent.contact

                val composer = screenContent.composer

                DirectUiState(
                    contactId = contactId,
                    contactName =
                        resolveContactName(
                            contact = contact,
                            conversation = conversation,
                            fallbackContactName = fallbackContactName
                        ),
                    messages = conversation?.messages?.reversed().orEmpty(),
                    messageText = composer.messageText,
                    isContactTyping = composer.isContactTyping,
                    contactSecurityState = contact.toSecurityState(),
                    identityHandshakeState = screenContent.chatContent.identityHandshakeState,
                    identitySetupMode = screenContent.chatContent.directIdentitySetupMode,
                    isLoading = contact == null,
                    isMessageInputEnabled =
                        isDirectChatAuthorized(
                            contact = contact,
                            identityHandshakeState = screenContent.chatContent.identityHandshakeState,
                            identitySetupMode = screenContent.chatContent.directIdentitySetupMode
                        ),
                    errorMessage = composer.errorMessage
                )
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
        observeIdentitySetupMode()
        observeIncomingTypingEvents()
        observeDeliveryTimeouts()
    }

    private fun observeIdentitySetupMode() {
        viewModelScope.launch {
            directIdentitySetupModeFlow
                .collect { mode ->
                    if (mode == DirectIdentitySetupMode.AUTOMATIC_INVITATION) {
                        ensureIdentityExchangeStarted(contactId)
                            .onFailure { error ->
                                errorMessage.value = error.message ?: "Contact invitation could not be started"
                            }
                    }
                }
        }
    }

    fun onUiEvent(event: DirectUiEvent) {
        when (event) {
            is DirectUiEvent.MessageTextChanged -> onMessageTextChanged(event.text)
            DirectUiEvent.SendClicked -> sendMessage()
            DirectUiEvent.HeaderClicked -> openContactDetails()
            is DirectUiEvent.RetryMessage -> retryMessage(event.messageId)
            DirectUiEvent.VerifyIdentityClicked -> verifyIdentity()
            DirectUiEvent.ManualIdentitySetupClicked -> Unit
            DirectUiEvent.ShareIdentityClicked -> shareIdentity()
            DirectUiEvent.ImportIdentityClicked -> importIdentity()
            DirectUiEvent.BackClicked -> navigateBack()
        }
    }

    private fun navigateBack() {
        navigator.popBackStackTo(AppRoute.Main)
    }

    private fun openContactDetails() {
        navigator.navigateTo(
            AppRoute.ContactDetails(
                conversationId = conversationId,
                contactId = contactId
            )
        )
    }

    private fun verifyIdentity() {
        navigator.navigateTo(
            AppRoute.ContactDetails(
                conversationId = conversationId,
                contactId = contactId,
                openVerification = true
            )
        )
    }

    private fun shareIdentity() {
        navigator.navigateTo(AppRoute.ShareIdentity)
    }

    private fun importIdentity() {
        navigator.navigateTo(AppRoute.ImportContact(contactId = contactId))
    }

    private fun onMessageTextChanged(value: String) {
        if (!uiState.value.isMessageInputEnabled) {
            return
        }

        messageText.value = value
        errorMessage.value = null

        localTypingStopJob?.cancel()
        localTypingStopJob = null

        if (value.isBlank()) {
            stopTyping()
            return
        }

        if (!isLocalTyping) {
            isLocalTyping = true
            sendTypingState(isTyping = true)
        }

        localTypingStopJob =
            viewModelScope.launch {
                delay(LOCAL_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                stopTypingNow()
            }
    }

    fun stopTyping() {
        localTypingStopJob?.cancel()
        localTypingStopJob = null

        if (!isLocalTyping) {
            return
        }

        isLocalTyping = false
        sendTypingState(isTyping = false)
    }

    private fun sendMessage() {
        if (!uiState.value.isMessageInputEnabled) {
            return
        }

        val normalizedText = messageText.value.trim()

        if (normalizedText.isEmpty()) {
            return
        }

        messageText.value = ""
        errorMessage.value = null
        stopTyping()

        viewModelScope.launch {
            runCatching {
                sendMessageUseCase(
                    conversationId = conversationId,
                    text = normalizedText
                )
            }.onFailure { error ->
                messageText.value = normalizedText
                errorMessage.value = error.message ?: "Message could not be sent"
            }
        }
    }

    private fun retryMessage(messageId: String) {
        if (messageId.isBlank()) {
            return
        }

        errorMessage.value = null

        viewModelScope.launch {
            retryFailedMessage(messageId)
                .onFailure { error ->
                    errorMessage.value = error.message ?: "Message could not be queued again"
                }
        }
    }

    fun markConversationRead() {
        viewModelScope.launch {
            markConversationReadUseCase(conversationId)
                .onFailure { error ->
                    logger.warn(error) { "Could not mark conversation as read" }
                }
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

    private fun observeIncomingTypingEvents() {
        viewModelScope.launch {
            observeTypingIndicator(contactId = contactId)
                .collect { isTyping ->
                    remoteTypingTimeoutJob?.cancel()
                    isContactTyping.value = isTyping

                    if (isTyping) {
                        remoteTypingTimeoutJob =
                            viewModelScope.launch {
                                delay(REMOTE_TYPING_TIMEOUT_MILLISECONDS.milliseconds)
                                isContactTyping.value = false
                            }
                    }
                }
        }
    }

    private fun sendTypingState(isTyping: Boolean) {
        viewModelScope.launch {
            sendTypingStateNow(isTyping = isTyping)
        }
    }

    private suspend fun sendTypingStateNow(isTyping: Boolean) {
        setTypingIndicator(
            contactId = contactId,
            isTyping = isTyping
        ).onFailure { error ->
            logger.warn(error) { "Could not send typing state for $contactId" }
        }
    }

    private suspend fun stopTypingNow() {
        if (!isLocalTyping) {
            return
        }

        isLocalTyping = false
        sendTypingStateNow(isTyping = false)
    }

    private data class ChatContentState(
        val conversation: Conversation?,
        val contact: Contact?,
        val identityHandshakeState: IdentityHandshakeState?,
        val directIdentitySetupMode: DirectIdentitySetupMode
    )

    private data class ComposerState(
        val messageText: String,
        val errorMessage: String?,
        val isContactTyping: Boolean
    )

    private data class ScreenContentState(
        val chatContent: ChatContentState,
        val composer: ComposerState
    )

    private companion object {
        const val DELIVERY_REFRESH_INTERVAL_MILLISECONDS = 15_000L
        const val LOCAL_TYPING_TIMEOUT_MILLISECONDS = 1500
        const val REMOTE_TYPING_TIMEOUT_MILLISECONDS = 3000
    }
}
